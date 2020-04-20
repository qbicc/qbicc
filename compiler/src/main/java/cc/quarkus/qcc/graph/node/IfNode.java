package cc.quarkus.qcc.graph.node;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.IfType;
import cc.quarkus.qcc.graph.type.IfValue;

public abstract class IfNode extends AbstractControlNode<IfType, IfValue> {

    public IfNode(ControlNode<?,?> control, CompareOp op) {
        super(control, IfType.INSTANCE);
        this.op = op;
    }

    public CompareOp getOp() {
        return this.op;
    }

    public IfTrueProjection getTrueOut() {
        return this.ifTrueOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IfTrueProjection(this )));
    }

    public IfFalseProjection getFalseOut() {
        return this.ifFalseOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IfFalseProjection(this )));
    }

    @Override
    public String label() {
        return "<if> " + this.op.label();
    }

    private final AtomicReference<IfTrueProjection> ifTrueOut = new AtomicReference<>();

    private final AtomicReference<IfFalseProjection> ifFalseOut = new AtomicReference<>();

    private final CompareOp op;

}
