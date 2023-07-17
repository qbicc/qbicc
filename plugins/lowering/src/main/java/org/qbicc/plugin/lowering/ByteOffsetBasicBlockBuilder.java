package org.qbicc.plugin.lowering;

import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;
import org.qbicc.type.StructType;
import org.qbicc.type.UnionType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;

/**
 * Some backends do not support typed pointer offset calculations.
 * On these backends, we lower typed offsets to byte offsets with explicit multiplies.
 * The multiplies are expected to be strength-reduced in other passes.
 */
public final class ByteOffsetBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public ByteOffsetBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public Value elementOf(Value arrayPointer, Value index) {
        BasicBlockBuilder fb = getFirstBuilder();
        LiteralFactory lf = getLiteralFactory();
        long size = arrayPointer.getPointeeType(ArrayType.class).getElementType().getSize();
        return fb.byteOffsetPointer(arrayPointer, fb.multiply(index, lf.literalOf(size)), arrayPointer.element().getType());
    }

    @Override
    public Value memberOf(Value structPointer, StructType.Member member) {
        BasicBlockBuilder fb = getFirstBuilder();
        LiteralFactory lf = getLiteralFactory();
        return fb.byteOffsetPointer(structPointer, lf.literalOf(member.getOffset()), member.getType());
    }

    @Override
    public Value memberOfUnion(Value unionPointer, UnionType.Member member) {
        BasicBlockBuilder fb = getFirstBuilder();
        return fb.bitCast(unionPointer, member.getType().getPointer());
    }

    @Override
    public Value offsetPointer(Value basePointer, Value offset) {
        BasicBlockBuilder fb = getFirstBuilder();
        LiteralFactory lf = getLiteralFactory();
        long size = basePointer.getPointeeType().getSize();
        return fb.byteOffsetPointer(basePointer, fb.multiply(offset, lf.literalOf(size)), basePointer.getPointeeType());
    }

    @Override
    public Value pointerDifference(Value leftPointer, Value rightPointer) {
        BasicBlockBuilder fb = getFirstBuilder();
        LiteralFactory lf = getLiteralFactory();
        UnsignedIntegerType intType = leftPointer.getType(PointerType.class).getSameSizedUnsignedInteger();
        ValueType leftPointeeType = leftPointer.getPointeeType();
        ValueType rightPointeeType = rightPointer.getPointeeType();
        // sanity check
        if (! leftPointeeType.equals(rightPointeeType)) {
            throw new IllegalArgumentException("Mismatched pointer types");
        }
        long sz = leftPointeeType.getSize();
        return fb.divide(fb.sub(fb.bitCast(leftPointer, intType), fb.bitCast(rightPointer, intType)), lf.literalOf(sz));
    }
}
