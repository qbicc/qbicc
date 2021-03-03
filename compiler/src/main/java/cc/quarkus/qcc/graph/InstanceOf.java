package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A node that represents a check of the upper bound of the value against the given type.
 */
public final class InstanceOf extends AbstractValue implements InstanceOperation {
    private final Value input;
    private final ValueType checkType;
    private final BooleanType booleanType;
    private final ObjectType classFileType;

    InstanceOf(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input, final ValueType checkType, final ObjectType classFileType, final BooleanType booleanType) {
        super(callSite, element, line, bci);
        this.input = input;
        this.checkType = checkType;
        this.classFileType = classFileType;
        this.booleanType = booleanType;
    }

    public ValueType getCheckType() {
        return checkType;
    }

    int calcHashCode() {
        return Objects.hash(input, checkType);
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceOf && equals((InstanceOf) other);
    }

    public boolean equals(final InstanceOf other) {
        return this == other || other != null && input.equals(other.input) && checkType.equals(other.checkType);
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? input : Util.throwIndexOutOfBounds(index);
    }

    public Value getInstance() {
        return input;
    }

    public BooleanType getType() {
        return booleanType;
    }
    
    public ObjectType getClassFileType() {
        return classFileType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
