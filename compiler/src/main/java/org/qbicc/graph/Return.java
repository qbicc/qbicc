package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Return extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final BasicBlock terminatedBlock;

    Return(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, Node dependency) {
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
