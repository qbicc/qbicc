package cc.quarkus.qcc.graph.node;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public abstract class IfNode extends AbstractControlNode<IfToken> {

    public IfNode(Graph<?> graph, ControlNode<?> control, CompareOp op) {
        super(graph, control, EphemeralTypeDescriptor.IF_TOKEN);
        this.op = op;
    }

    public CompareOp getOp() {
        return this.op;
    }

    public IfTrueProjection getTrueOut() {
        return this.ifTrueOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IfTrueProjection(getGraph(), this )));
    }

    public IfFalseProjection getFalseOut() {
        return this.ifFalseOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new IfFalseProjection(getGraph(), this )));
    }

    @Override
    public String label() {
        return "<if> " + this.op.label();
    }

    private final AtomicReference<IfTrueProjection> ifTrueOut = new AtomicReference<>();

    private final AtomicReference<IfFalseProjection> ifFalseOut = new AtomicReference<>();

    private final CompareOp op;

}
