package cc.quarkus.qcc.graph;

/**
 *
 */
final class TryThrowImpl extends ThrowImpl implements TryThrow {
    final CatchValueImpl catchValue = new CatchValueImpl();
    NodeHandle catchHandler;

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

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }
}
