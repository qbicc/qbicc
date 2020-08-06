package cc.quarkus.qcc.graph;

class TryInvocationImpl extends InvocationImpl implements TryInvocation {
    // a hard reference
    final CatchValueImpl catchValue = new CatchValueImpl();
    NodeHandle catchHandler;
    NodeHandle normalTarget;

    TryInvocationImpl() {
    }

    public Value getCatchValue() {
        return catchValue;
    }

    public BasicBlock getCatchHandler() {
        return NodeHandle.getTargetOf(catchHandler);
    }

    public void setCatchHandler(final BasicBlock catchHandler) {
        this.catchHandler = NodeHandle.of(catchHandler);
    }

    void setCatchHandler(final NodeHandle catchHandler) {
        this.catchHandler = catchHandler;
    }

    public BasicBlock getNextBlock() {
        return NodeHandle.getTargetOf(normalTarget);
    }

    public void setNextBlock(final BasicBlock branch) {
        this.normalTarget = NodeHandle.of(branch);
    }

    void setNextBlock(final NodeHandle branch) {
        this.normalTarget = branch;
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }
}
