package org.qbicc.graph;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class If extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value condition;
    private final BlockLabel trueBranchLabel;
    private final BlockLabel falseBranchLabel;
    private final BasicBlock terminatedBlock;

    If(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final Value condition, final BlockLabel trueBranchLabel, final BlockLabel falseBranchLabel, SortedMap<Slot, BlockParameter> parameters, Map<Slot, Value> blockArgs) {
        super(callSite, element, line, bci, blockArgs);
        terminatedBlock = new BasicBlock(blockEntry, this, parameters);
        this.dependency = dependency;
        this.condition = condition;
        this.trueBranchLabel = trueBranchLabel;
        this.falseBranchLabel = falseBranchLabel;
    }

    @Override
    public Value getOutboundValue(PhiValue phi) {
        Value outboundValue = super.getOutboundValue(phi);
        if (phi.getPinnedBlock().equals(getTrueBranch())) {
            return condition.getValueIfTrue(outboundValue);
        } else if (phi.getPinnedBlock().equals(getFalseBranch())) {
            return condition.getValueIfFalse(outboundValue);
        }
        return outboundValue;
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
        condition.toString(b);
        b.append(')');
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
