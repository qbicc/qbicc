package org.qbicc.plugin.gc.nogc;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.plugin.coreclasses.BasicHeaderInitializer;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
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
                ptrVal = valueConvert(stackAllocate(compoundType, lf.literalOf(1), align), type.getReference());
            }
        }
        if (ptrVal == null) {
            MethodElement method = noGc.getAllocateMethod();
            ptrVal = notNull(call(staticMethod(method), List.of(size, align)));
        }

        // zero initialize the allocated storage
        MethodElement method = noGc.getZeroMethod();
        call(staticMethod(method), List.of(ptrVal, size));

        Value oop = valueConvert(ptrVal, type.getReference());
        BasicHeaderInitializer.initializeObjectHeader(ctxt, this, referenceHandle(oop), typeId);
        return oop;
    }

    @Override
    public Value newArray(final PrimitiveArrayObjectType arrayType, Value size) {
        LoadedTypeDefinition ltd = coreClasses.getArrayContentField(arrayType).getEnclosingType().load();
        CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(ltd).getCompoundType();
        Value ptrVal = allocateArray(compoundType, size, arrayType.getElementType().getSize());
        Value oop = valueConvert(ptrVal, arrayType.getReference());
        BasicHeaderInitializer.initializeArrayHeader(ctxt, this, referenceHandle(oop), ctxt.getLiteralFactory().literalOfType(ltd.getClassType()), size);
        return oop;
    }

    @Override
    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, Value size) {
        Layout layout = Layout.get(ctxt);
        LayoutInfo info = layout.getInstanceLayoutInfo(coreClasses.getRefArrayContentField().getEnclosingType());
        CompoundType compoundType = info.getCompoundType();
        Value ptrVal = allocateArray(compoundType, size, ctxt.getTypeSystem().getReferenceSize());
        Value oop = valueConvert(ptrVal, arrayType.getReference());
        BasicHeaderInitializer.initializeRefArrayHeader(ctxt, this, referenceHandle(oop), elemTypeId, dimensions, size);
        return oop;
    }

    public Node safePoint() {
        // No safepoints in NoGC
        return nop();
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
        Value ptrVal = notNull(call(staticMethod(method1), List.of(realSize, align)));
        MethodElement method = noGc.getZeroMethod();
        call(staticMethod(method), List.of(ptrVal, realSize));

        return ptrVal;
    }
}
