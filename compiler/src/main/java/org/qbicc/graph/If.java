package org.qbicc.graph;

import java.util.Map;
import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public final class If extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value condition;
    private final BlockLabel trueBranchLabel;
    private final BlockLabel falseBranchLabel;
    private final BasicBlock terminatedBlock;

    If(final ProgramLocatable pl, final BlockEntry blockEntry, final Node dependency, final Value condition, final BlockLabel trueBranchLabel, final BlockLabel falseBranchLabel, Map<Slot, Value> blockArgs) {
        super(pl, blockArgs);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.condition = condition;
        this.trueBranchLabel = trueBranchLabel;
        this.falseBranchLabel = falseBranchLabel;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public final Value getCondition() {
        return condition;
    }

    public BlockLabel getTrueBranchLabel() {
        return trueBranchLabel;
    }

    public BasicBlock getTrueBranch() {
        return BlockLabel.getTargetOf(trueBranchLabel);
    }

    public BlockLabel getFalseBranchLabel() {
        return falseBranchLabel;
    }

    public BasicBlock getFalseBranch() {
        return BlockLabel.getTargetOf(falseBranchLabel);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getCondition() : Util.throwIndexOutOfBounds(index);
    }

    public int getSuccessorCount() {
        return 2;
    }

    public BasicBlock getSuccessor(final int index) {
        return index == 0 ? getTrueBranch() : index == 1 ? getFalseBranch() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(If.class, dependency, condition, trueBranchLabel, falseBranchLabel);
    }

    @Override
    String getNodeName() {
        return "If";
    }

    public boolean equals(final Object other) {
        return other instanceof If && equals((If) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        condition.toReferenceString(b);
        b.append(')');
        if (trueBranchLabel.hasTarget() && falseBranchLabel.hasTarget()) {
            b.append(' ');
            b.append("then");
            b.append(' ');
            getTrueBranch().toString(b);
            b.append(' ');
            b.append("else");
            b.append(' ');
            getFalseBranch().toString(b);
        }
        return b;
    }

    public boolean equals(final If other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && condition.equals(other.condition)
            && trueBranchLabel.equals(other.trueBranchLabel)
            && falseBranchLabel.equals(other.falseBranchLabel);
    }
}
