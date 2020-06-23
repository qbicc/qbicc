package cc.quarkus.qcc.graph;

final class TryInstanceFieldWriteImpl extends InstanceFieldWriteImpl implements TryInstanceFieldWrite {
    final CatchValueImpl catchValue = new CatchValueImpl();
    NodeHandle catchHandler;
    NodeHandle normalTarget;

    public Value getCatchValue() {
        return catchValue;
    }

    public void setOwner(final BasicBlock owner) {
        catchValue.setOwner(owner);
        super.setOwner(owner);
    }

    public BasicBlock getCatchHandler() {
        return NodeHandle.getTargetOf(catchHandler);
    }

    public void setCatchHandler(final BasicBlock catchHandler) {
        this.catchHandler = NodeHandle.of(catchHandler);
    }

    public BasicBlock getNextBlock() {
        return NodeHandle.getTargetOf(normalTarget);
    }

    public void setNextBlock(final BasicBlock branch) {
        this.normalTarget = NodeHandle.of(branch);
    }
}
