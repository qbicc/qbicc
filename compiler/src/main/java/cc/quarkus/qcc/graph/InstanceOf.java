package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A node that represents a check of the upper bound of the value against the given type.
 */
public final class InstanceOf extends AbstractValue implements InstanceOperation {
    private final Value input;
    private final ObjectType checkType;
    private final IntegerLiteral checkDimensions;
    private final BooleanType booleanType;

    InstanceOf(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input,
               final ObjectType checkType, final IntegerLiteral checkDimensions, final BooleanType booleanType) {
        super(callSite, element, line, bci);
        this.input = input;
        this.checkType = checkType;
        this.checkDimensions = checkDimensions;
        this.booleanType = booleanType;
    }

    public ObjectType getCheckType() {
        return checkType;
    }

    public IntegerLiteral getCheckDimensions() { return checkDimensions; }

    int calcHashCode() {
        return Objects.hash(input, checkType, checkDimensions);
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceOf && equals((InstanceOf) other);
    }

    public boolean equals(final InstanceOf other) {
        return this == other || other != null && input.equals(other.input) && checkType.equals(other.checkType) && checkDimensions.equals(other.checkDimensions);
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? input : index == 1 ? checkDimensions : Util.throwIndexOutOfBounds(index);
    }

    public Value getInstance() {
        return input;
    }

    public BooleanType getType() {
        return booleanType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
