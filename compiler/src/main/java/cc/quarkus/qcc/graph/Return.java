package cc.quarkus.qcc.graph;

import java.util.Objects;

/**
 *
 */
public final class Return extends AbstractNode implements Terminator {
    private final Node dependency;

    Return(final int line, final int bci, Node dependency) {
        super(line, bci);
        this.dependency = dependency;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Return.class, dependency);
    }

    public boolean equals(final Object other) {
        return other instanceof Return && equals((Return) other);
    }

    public boolean equals(final Return other) {
        return this == other || other != null
            && dependency.equals(other.dependency);
    }
}
