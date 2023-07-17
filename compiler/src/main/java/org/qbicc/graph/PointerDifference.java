package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PointerType;
import org.qbicc.type.SignedIntegerType;

/**
 * A binary value which represents the number of elements between two pointers of the same type.
 * This is the inverse operation to {@link OffsetPointer}.
 * The output type is a signed integer whose size is equal to the size of the input pointers.
 */
public final class PointerDifference extends AbstractBinaryValue implements NonCommutativeBinaryValue {
    private final SignedIntegerType type;

    PointerDifference(ProgramLocatable pl, Value left, Value right) {
        super(pl, left, right);
        PointerType leftType = left.getType(PointerType.class);
        PointerType rightType = right.getType(PointerType.class);
        if (! leftType.equals(rightType)) {
            throw new IllegalStateException();
        }
        type = leftType.getSameSizedSignedInteger();
    }

    @Override
    String getNodeName() {
        return "PointerDifference";
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        b.append("ptrdiff (");
        getLeftInput().toReferenceString(b);
        b.append(" - ");
        getRightInput().toReferenceString(b);
        b.append(')');
        return b;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public SignedIntegerType getType() {
        return type;
    }
}
