package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.SignedIntegerType;

public class CmpG extends AbstractBinaryValue implements NonCommutativeBinaryValue {
    private final SignedIntegerType integerType;

    CmpG(final ProgramLocatable pl, final Value v1, final Value v2, SignedIntegerType integerType) {
        super(pl, v1, v2);
        this.integerType = integerType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public SignedIntegerType getType() {
        return integerType;
    }

    @Override
    String getNodeName() {
        return "CmpG";
    }
}
