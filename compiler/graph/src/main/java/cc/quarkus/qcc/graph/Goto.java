package cc.quarkus.qcc.graph;

/**
 * A terminator which designates a subsequent block for normal execution.
 */
public final class Goto extends AbstractNode implements Resume {
    private final Node dependency;
    private final BlockLabel targetLabel;

    Goto(Node dependency, BlockLabel targetLabel) {
        this.dependency = dependency;
        this.targetLabel = targetLabel;
    }

    public BlockLabel getResumeTargetLabel() {
        return targetLabel;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
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
}
