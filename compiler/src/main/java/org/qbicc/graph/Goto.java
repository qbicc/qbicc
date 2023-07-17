package org.qbicc.graph;

import java.util.Map;
import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 * A terminator which designates a subsequent block for normal execution.
 */
public final class Goto extends AbstractTerminator implements Resume {
    private final Node dependency;
    private final BlockLabel targetLabel;
    private final BasicBlock terminatedBlock;

    Goto(final ProgramLocatable pl, final BlockEntry blockEntry, Node dependency, BlockLabel targetLabel, Map<Slot, Value> targetArguments) {
        super(pl, targetArguments);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.targetLabel = targetLabel;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public BlockLabel getResumeTargetLabel() {
        return targetLabel;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public int getSuccessorCount() {
        return 1;
    }

    public BasicBlock getSuccessor(final int index) {
        return index == 0 ? getResumeTarget() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Goto.class, dependency, targetLabel);
    }

    @Override
    String getNodeName() {
        return "Goto";
    }

    public boolean equals(final Object other) {
        return other instanceof Goto && equals((Goto) other);
    }

    public boolean equals(final Goto other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && targetLabel.equals(other.targetLabel);
    }
}
