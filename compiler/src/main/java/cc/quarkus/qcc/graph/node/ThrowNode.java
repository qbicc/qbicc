package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ThrowToken;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ThrowNode extends AbstractControlNode<ThrowToken> {

    public ThrowNode(Graph<?> graph, ControlNode<?> control) {
        super(graph, control, TypeDescriptor.EphemeralTypeDescriptor.THROW_TOKEN);
    }

    @Override
    public ThrowToken getValue(Context context) {
        ObjectReference thrown = context.get(this.thrown);
        return new ThrowToken(thrown);
    }

    public void setThrown(Node<ObjectReference> thrown) {
        this.thrown = thrown;
        thrown.addSuccessor(this);
    }

    public Node<ObjectReference> getThrown() {
        return this.thrown;
    }

    public ThrowControlProjection getThrowControlOut() {
        return this.throwControlOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ThrowControlProjection(getGraph(), this)));
    }

    public ExceptionProjection getExceptionOut() {
        return this.exceptionOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ExceptionProjection(getGraph(), this)));
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add(getControl());
            add(getThrown());
        }};
    }

    @Override
    public String label() {
        return "<throw:" + getId() + "> " + ( this.thrown == null ? "" : this.thrown.label() );
    }

    private Node<ObjectReference> thrown;

    private AtomicReference<ThrowControlProjection> throwControlOut = new AtomicReference<>();

    private AtomicReference<ExceptionProjection> exceptionOut = new AtomicReference<>();
}
