package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

public class Unreachable extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;

    Unreachable(final ProgramLocatable pl, final BlockEntry blockEntry, Node dependency) {
        super(pl);
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

    @Override
    String getNodeName() {
        return "Unreachable";
    }

    public boolean equals(final Object other) {
        return other instanceof Unreachable && equals((Unreachable) other);
    }

    public boolean equals(final Unreachable other) {
        return this == other || other != null
                && dependency.equals(other.dependency);
    }
}
