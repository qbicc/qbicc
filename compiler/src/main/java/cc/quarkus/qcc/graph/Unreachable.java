package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

public class Unreachable extends AbstractNode implements Terminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;

    Unreachable(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, Node dependency) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        terminatedBlock = new BasicBlock(blockEntry, this);
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    @Override
    public Node getDependency() {
        return dependency;
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
