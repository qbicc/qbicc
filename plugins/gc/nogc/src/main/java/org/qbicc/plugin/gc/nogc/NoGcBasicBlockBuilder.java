package org.qbicc.plugin.gc.nogc;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public class NoGcBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final CoreClasses coreClasses;

    public NoGcBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        this.coreClasses = CoreClasses.get(ctxt);
    }

    @Override
    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        NoGc noGc = NoGc.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();
        Value ptrVal = null;
        if (typeId instanceof TypeLiteral tl && tl.getValue() instanceof ClassObjectType cot) {
            // We can only even attempt stack allocation if the typeId is a literal (ie, known precisely at compile time).
            if (cot.isSubtypeOf(noGc.getStackObjectType()) /*|| objectDoesNotEscape && objectIsSmallEnough */) {
                CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(cot.getDefinition()).getCompoundType();
                ptrVal = stackAllocate(compoundType, lf.literalOf(1), align);
            }
        }
        if (ptrVal == null) {
            MethodElement method = noGc.getAllocateMethod();
            ptrVal = notNull(call(staticMethod(method, method.getDescriptor(), method.getType()), List.of(size, align)));
        }

        // zero initialize the allocated storage
        MethodElement method = noGc.getZeroMethod();
        call(staticMethod(method, method.getDescriptor(), method.getType()), List.of(ptrVal, size));

        return valueConvert(ptrVal, type.getReference());
    }

    @Override
    public Value newArray(final PrimitiveArrayObjectType arrayType, Value size) {
        Layout layout = Layout.get(ctxt);
        LayoutInfo info = layout.getInstanceLayoutInfo(coreClasses.getArrayContentField(arrayType).getEnclosingType());
        CompoundType compoundType = info.getCompoundType();
        Value ptrVal = allocateArray(compoundType, size, arrayType.getElementType().getSize());
        return valueConvert(ptrVal, arrayType.getReference());
    }

    @Override
    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, Value size) {
        Layout layout = Layout.get(ctxt);
        LayoutInfo info = layout.getInstanceLayoutInfo(coreClasses.getRefArrayContentField().getEnclosingType());
        CompoundType compoundType = info.getCompoundType();
        Value ptrVal = allocateArray(compoundType, size, ctxt.getTypeSystem().getReferenceSize());
        return valueConvert(ptrVal, arrayType.getReference());
    }

    private Value allocateArray(CompoundType compoundType, Value size, long elementSize) {
        NoGc noGc = NoGc.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());
        IntegerLiteral baseSize = lf.literalOf(compoundType.getSize());
        IntegerType sizeType = (IntegerType) size.getType();
        if (sizeType.getMinBits() < 64) {
            size = extend(size, ctxt.getTypeSystem().getSignedInteger64Type());
        }
        assert Long.bitCount(elementSize) == 1;
        int elementShift = Long.numberOfTrailingZeros(elementSize);
        Value realSize = add(baseSize, elementShift == 0 ? size : shl(size, lf.literalOf((IntegerType)size.getType(), elementShift)));

        // Allocate and zero-initialize the storage
        MethodElement method1 = noGc.getAllocateMethod();
        Value ptrVal = notNull(call(staticMethod(method1, method1.getDescriptor(), method1.getType()), List.of(realSize, align)));
        MethodElement method = noGc.getZeroMethod();
        call(staticMethod(method, method.getDescriptor(), method.getType()), List.of(ptrVal, realSize));

        return ptrVal;
    }

    public Value clone(final Value object) {
        ValueType objType = object.getType();
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        if (objType instanceof ClassObjectType) {
            // TODO: This implementation is actually not correct, because it is cloning based
            //       on the static type of the value; not the actual runtime type.
            //       I would remove it entirely, except it is actually needed to pass one of our
            //       integration tests (which is using Enums).
            ClassObjectType type = (ClassObjectType) objType;
            LayoutInfo info = layout.getInstanceLayoutInfo(type.getDefinition());
            LiteralFactory lf = ctxt.getLiteralFactory();
            CompoundType compoundType = info.getCompoundType();
            IntegerLiteral size = lf.literalOf(compoundType.getSize());
            IntegerLiteral align = lf.literalOf(compoundType.getAlign());
            Value ptrVal;
            if (type.isSubtypeOf(noGc.getStackObjectType())) {
                ptrVal = stackAllocate(compoundType, lf.literalOf(1), align);
            } else {
                MethodElement method = noGc.getAllocateMethod();
                ptrVal = notNull(call(staticMethod(method, method.getDescriptor(), method.getType()), List.of(size, align)));
            }
            // TODO: replace with field-by-field copy once we have a redundant assignment elimination optimization
            // TODO: if/when we put a thinlock, default hashcode, or GC state bits in the object header we need to properly initialize them.
            MethodElement method = noGc.getCopyMethod();
            call(staticMethod(method, method.getDescriptor(), method.getType()), List.of(ptrVal, valueConvert(object, (WordType) ptrVal.getType()), size));
            fence(MemoryAtomicityMode.RELEASE);
            return valueConvert(ptrVal, type.getReference());
        } else if (objType instanceof ArrayObjectType) {
            ctxt.error(getLocation(), "Array allocations not supported until layout supports arrays");
            throw new BlockEarlyTermination(unreachable());
        } else {
            return super.clone(object);
        }
    }
}
