package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class Reachable extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final Value reachableValue;

    Reachable(final ProgramLocatable pl, Node dependency, Value reachableValue) {
        super(pl);
        this.dependency = dependency;
        this.reachableValue = reachableValue;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> reachableValue;
            default -> Util.throwIndexOutOfBounds(index);
        };
    }

    public Value getReachableValue() {
        return reachableValue;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    String getNodeName() {
        return "Reachable";
    }

    @Override
    int calcHashCode() {
        return Objects.hash(Reachable.class, dependency, reachableValue);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return reachableValue.toReferenceString(super.toString(b).append('(')).append(')');
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Reachable r && equals(r);
    }

    public boolean equals(Reachable other) {
        return this == other || other != null && dependency.equals(other.dependency) && reachableValue.equals(other.reachableValue);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
