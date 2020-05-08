package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ThrowSource;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ExceptionProjection extends AbstractNode<ObjectReference> implements Projection {

    protected <T extends ControlNode<? extends ThrowSource>> ExceptionProjection(Graph<?> graph, T control) {
        super(graph, control, TypeDescriptor.THROWABLE);
    }

    @SuppressWarnings("unchecked")
    ControlNode<? extends ThrowSource> getThrowSource() {
        return (ControlNode<? extends ThrowSource>) getControl();
    }

    @Override
    public ObjectReference getValue(Context context) {
        ThrowSource src = context.get(getThrowSource());
        return src.getThrowValue();
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<exception: " + getId() + "> " + getThrowSource().label();
    }
}
