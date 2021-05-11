package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.BooleanType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A node that represents a check of the upper bound of the value against the given type.
 */
public final class InstanceOf extends AbstractValue implements InstanceOperation, OrderedNode, BooleanValue {
    private final Node dependency;
    private final Value input;
    private final ObjectType checkType;
    private final int checkDimensions;
    private final BooleanType booleanType;

    InstanceOf(final Node callSite, final ExecutableElement element, final int line, final int bci, Node dependency, final Value input,
               final ObjectType checkType, final int checkDimensions, final BooleanType booleanType) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.input = input;
        this.checkType = checkType;
        this.checkDimensions = checkDimensions;
        this.booleanType = booleanType;
    }

    public ObjectType getCheckType() {
        return checkType;
    }

    public int getCheckDimensions() {
        return checkDimensions; 
     }

    int calcHashCode() {
        return Objects.hash(dependency, input, checkType, checkDimensions);
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceOf && equals((InstanceOf) other);
    }

    public boolean equals(final InstanceOf other) {
        return this == other || other != null && dependency.equals(other.dependency) && input.equals(other.input) && checkType.equals(other.checkType) && checkDimensions == other.checkDimensions;
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

    @Override
    public Value getValueIfTrue(Value input) {
        return input.equals(this.input) ?
            new NotNull(getCallSite(), getElement(), getSourceLine(), getBytecodeIndex(),
                new BitCast(getCallSite(), getElement(), getSourceLine(), getBytecodeIndex(), input, ((ReferenceType)input.getType()).narrow(checkType)))
            : input;
    }

    @Override
    public Value getValueIfFalse(Value input) {
        return input;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }
}
