package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.InvokeValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class ResultProjection<T extends ConcreteType<T>, V extends Value<T,V>> extends AbstractNode<T, V>{

    protected ResultProjection(InvokeNode in, T outType) {
        super(in, outType);
    }

    @Override
    public InvokeNode getControl() {
        return (InvokeNode) super.getControl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getValue(Context context) {
        InvokeValue input = context.get(getControl());
        return (V) input.getReturnValue();
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> result: " + getType().label();
    }
}
