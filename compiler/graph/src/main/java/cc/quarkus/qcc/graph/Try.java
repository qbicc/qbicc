package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;

/**
 * An operation which may throw an exception.
 */
public final class Try extends AbstractNode implements Resume {
    private final Node dependency;
    private final Triable delegateOperation;
    private final List<ClassTypeIdLiteral> catchTypeIds;
    private final List<BlockLabel> catchTargetLabels;
    private final BlockLabel resumeTargetLabel;

    Try(final Node dependency, final Triable delegateOperation, final List<ClassTypeIdLiteral> catchTypeIds, final List<BlockLabel> catchTargetLabels, final BlockLabel resumeTargetLabel) {
        this.dependency = dependency;
        this.delegateOperation = delegateOperation;
        this.catchTypeIds = catchTypeIds;
        this.catchTargetLabels = catchTargetLabels;
        this.resumeTargetLabel = resumeTargetLabel;
    }

    public Triable getDelegateOperation() {
        return delegateOperation;
    }

    public int getCatchTypeIdCount() {
        return catchTypeIds.size();
    }

    public ClassTypeIdLiteral getCatchTypeId(int index) {
        return catchTypeIds.get(index);
    }

    public List<ClassTypeIdLiteral> getCatchTypeIds() {
        return catchTypeIds;
    }

    public BasicBlock getCatchTarget(int index) {
        return BlockLabel.getTargetOf(catchTargetLabels.get(index));
    }

    public BlockLabel getCatchTargetLabel(int index) {
        return catchTargetLabels.get(index);
    }

    public BlockLabel getResumeTargetLabel() {
        return resumeTargetLabel;
    }

    public List<BlockLabel> getCatchTargetLabels() {
        return catchTargetLabels;
    }

    public int getBasicDependencyCount() {
        return 2;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : index == 1 ? delegateOperation : Util.throwIndexOutOfBounds(index);
    }

    public int getSuccessorCount() {
        return 1 + catchTargetLabels.size();
    }

    public BasicBlock getSuccessor(final int index) {
        int length = catchTargetLabels.size();
        return index < length ? getCatchTarget(index) : index == length ? getResumeTarget() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
