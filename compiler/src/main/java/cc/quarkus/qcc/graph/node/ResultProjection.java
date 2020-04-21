package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.InvokeValue;
import cc.quarkus.qcc.interpret.Context;

public class ResultProjection<V> extends AbstractNode<V>{

    protected ResultProjection(InvokeNode<V> in, Class<V> outType) {
        super(in, outType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public InvokeNode<V> getControl() {
        return (InvokeNode<V>) super.getControl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getValue(Context context) {
        InvokeValue input = context.get(getControl());
        return (V) input.getReturnValue();
    }

    @Override
    public List<Node< ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

}
