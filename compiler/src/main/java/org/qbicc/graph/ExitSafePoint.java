package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 * Exit a safepoint. Usually paired with an {@link EnterSafePoint} node.
 */
public final class ExitSafePoint extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final Value setBits;
    private final Value clearBits;

    ExitSafePoint(final ProgramLocatable pl, Node dependency, Value setBits, Value clearBits) {
        super(pl);
        this.dependency = dependency;
        this.setBits = setBits;
        this.clearBits = clearBits;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return true;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> setBits;
            case 1 -> clearBits;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public Value setBits() {
        return setBits;
    }

    public Value clearBits() {
        return clearBits;
    }

    @Override
    String getNodeName() {
        return "exitSafePoint";
    }

    @Override
    int calcHashCode() {
        return Objects.hash(ExitSafePoint.class, dependency, setBits, clearBits);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExitSafePoint r && equals(r);
    }

    public boolean equals(ExitSafePoint other) {
        return this == other || other != null && dependency.equals(other.dependency) && setBits.equals(other.setBits) && clearBits.equals(other.clearBits);
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
