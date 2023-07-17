package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.BooleanType;

/**
 *
 */
public final class IsLt extends AbstractBooleanCompare implements NonCommutativeBinaryValue {
    IsLt(final ProgramLocatable pl, final Value v1, final Value v2, final BooleanType booleanType) {
        super(pl, v1, v2, booleanType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "IsLt";
    }
}
