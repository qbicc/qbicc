package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public abstract class AbstractCmp extends AbstractBinaryValue {
    private final BooleanType booleanType;

    AbstractCmp(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value left, final Value right, final BooleanType booleanType) {
        super(callSite, element, line, bci, left, right);
        this.booleanType = booleanType;
    }

    public ValueType getType() {
        return booleanType;
    }
}
