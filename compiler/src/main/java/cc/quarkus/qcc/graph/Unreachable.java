package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.Element;

import java.util.Objects;

public class Unreachable extends AbstractNode implements Terminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;

    Unreachable(final Element element, final int line, final int bci, final BlockEntry blockEntry, Node dependency) {
        super(element, line, bci);
        this.dependency = dependency;
        terminatedBlock = new BasicBlock(blockEntry, this);
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
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
        return Objects.hash(Unreachable.class, dependency);
    }

    public boolean equals(final Object other) {
        return other instanceof Unreachable && equals((Unreachable) other);
    }

    public boolean equals(final Unreachable other) {
        return this == other || other != null
                && dependency.equals(other.dependency);
    }
}
