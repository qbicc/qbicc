package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.BooleanType;

/**
 *
 */
public abstract class AbstractBooleanCompare extends AbstractBinaryValue implements BooleanValue {
    private final BooleanType booleanType;

    AbstractBooleanCompare(final ProgramLocatable pl, final Value left, final Value right, final BooleanType booleanType) {
        super(pl, left, right);
        this.booleanType = booleanType;
    }

    @Override
    public BooleanType getType() {
        return booleanType;
    }
}
