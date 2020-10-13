package cc.quarkus.qcc.graph;

/**
 *
 */
public final class If extends AbstractNode implements Terminator {
    private final Node dependency;
    private final Value condition;
    private final BlockLabel trueBranchLabel;
    private final BlockLabel falseBranchLabel;

    If(final Node dependency, final Value condition, final BlockLabel trueBranchLabel, final BlockLabel falseBranchLabel) {
        this.dependency = dependency;
        this.condition = condition;
        this.trueBranchLabel = trueBranchLabel;
        this.falseBranchLabel = falseBranchLabel;
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
}
