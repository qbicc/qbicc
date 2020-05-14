package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class ResultProjection<V extends QType> extends AbstractNode<V> implements Projection {

    protected ResultProjection(Graph<?> graph, InvokeNode<V> in, TypeDescriptor<V> outType) {
        super(graph, in, outType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public InvokeNode<V> getControl() {
        return (InvokeNode<V>) super.getControl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getValue(Context context) {
        InvokeToken input = context.get(getControl());
        return (V) input.getReturnValue();
    }

    @Override
    public List<Node< ?>> getPredecessors() {
        return List.of(getControl());
    }

}
