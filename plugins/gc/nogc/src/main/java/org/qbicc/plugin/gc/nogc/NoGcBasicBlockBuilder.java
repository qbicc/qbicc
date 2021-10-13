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
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.FieldElement;

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

    public Value new_(final ClassObjectType type) {
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        LayoutInfo info = layout.getInstanceLayoutInfo(type.getDefinition());
        CompoundType compoundType = info.getCompoundType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());
        Value ptrVal;
        if (type.isSubtypeOf(noGc.getStackObjectType()) /*|| objectDoesNotEscape && objectIsSmallEnough */) {
            ptrVal = stackAllocate(compoundType, lf.literalOf(1), align);
        } else {
            long size = compoundType.getSize();
            ptrVal = notNull(call(staticMethod(noGc.getAllocateMethod()), List.of(lf.literalOf(size), align)));
        }
        Value oop = valueConvert(ptrVal, type.getReference());

        // zero initialize the object's instance fields
        call(staticMethod(noGc.getZeroMethod()), List.of(ptrVal, lf.literalOf(info.getCompoundType().getSize())));

        return oop;
    }

    public Value newArray(final ArrayObjectType arrayType, Value size) {
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        FieldElement arrayContentField = coreClasses.getArrayContentField(arrayType);
        LayoutInfo info = layout.getInstanceLayoutInfo(arrayContentField.getEnclosingType());
        CompoundType compoundType = info.getCompoundType();
        LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerLiteral align = lf.literalOf(compoundType.getAlign());
        IntegerLiteral baseSize = lf.literalOf(compoundType.getSize());
        IntegerType sizeType = (IntegerType) size.getType();
        if (sizeType.getMinBits() < 64) {
            size = extend(size, ctxt.getTypeSystem().getSignedInteger64Type());
        }
        long elementSize = arrayType.getElementType().getSize();
        assert Long.bitCount(elementSize) == 1;
        int elementShift = Long.numberOfTrailingZeros(elementSize);
        Value realSize = add(baseSize, elementShift == 0 ? size : shl(size, lf.literalOf((IntegerType)size.getType(), elementShift)));
        Value ptrVal = notNull(call(staticMethod(noGc.getAllocateMethod()), List.of(realSize, align)));

        call(staticMethod(noGc.getZeroMethod()), List.of(ptrVal, realSize));

        return valueConvert(ptrVal, arrayType.getReference());
    }

    public Value clone(final Value object) {
        ValueType objType = object.getType();
        NoGc noGc = NoGc.get(ctxt);
        Layout layout = Layout.get(ctxt);
        if (objType instanceof ClassObjectType) {
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
                ptrVal = notNull(call(staticMethod(noGc.getAllocateMethod()), List.of(size, align)));
            }
            // TODO: replace with field-by-field copy once we have a redundant assignment elimination optimization
            // TODO: if/when we put a thinlock, default hashcode, or GC state bits in the object header we need to properly initialize them.
            call(staticMethod(noGc.getCopyMethod()), List.of(ptrVal, valueConvert(object, (WordType) ptrVal.getType()), size));
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
