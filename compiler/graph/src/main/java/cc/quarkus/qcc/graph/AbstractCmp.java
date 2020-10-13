package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public abstract class AbstractCmp extends AbstractBinaryValue {
    private final BooleanType booleanType;

    AbstractCmp(final Value left, final Value right, final BooleanType booleanType) {
        super(left, right);
        this.booleanType = booleanType;
    }

    public ValueType getType() {
        return booleanType;
    }
}
