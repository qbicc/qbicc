package cc.quarkus.qcc.graph;

import java.util.Objects;

/**
 * A return which returns a non-{@code void} value.
 */
public final class ValueReturn extends AbstractNode implements Terminator {
    private final Node dependency;
    private final Value returnValue;

    ValueReturn(final int line, final int bci, final Node dependency, final Value returnValue) {
        super(line, bci);
        this.dependency = dependency;
        this.returnValue = returnValue;
    }

    public Value getReturnValue() {
        return returnValue;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getReturnValue() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(ValueReturn.class, dependency, returnValue);
    }

    public boolean equals(final Object other) {
        return other instanceof ValueReturn && equals((ValueReturn) other);
    }

    public boolean equals(final ValueReturn other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && returnValue.equals(other.returnValue);
    }
}
