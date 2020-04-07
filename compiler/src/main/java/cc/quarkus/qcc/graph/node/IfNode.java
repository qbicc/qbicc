package cc.quarkus.qcc.graph.node;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.BooleanType;

public abstract class IfNode extends ControlNode<IfType> {

    public IfNode(ControlNode<?> control, CompareOp op) {
        super(control, IfType.INSTANCE);
        this.op = op;
    }

    public IfTrueProjection getTrueOut() {
        return this.ifTrueOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IfTrueProjection(this)));
    }

    public IfFalseProjection getFalseOut() {
        return this.ifFalseOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IfFalseProjection(this)));
    }

    private AtomicReference<IfTrueProjection> ifTrueOut = new AtomicReference<>();
    private AtomicReference<IfFalseProjection> ifFalseOut = new AtomicReference<>();

    private final CompareOp op;

}
