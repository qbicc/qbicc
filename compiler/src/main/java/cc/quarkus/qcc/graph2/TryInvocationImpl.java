package cc.quarkus.qcc.graph2;

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

    public void setOwner(final BasicBlock owner) {
        catchValue.setOwner(owner);
        super.setOwner(owner);
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
