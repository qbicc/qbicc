package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.StartToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ParameterProjection<V> extends AbstractNode<V> implements Projection {

    protected ParameterProjection(StartNode in, TypeDescriptor<V> outType, int index) {
        super(in, outType);
        this.index = index;
    }

    @Override
    public StartNode getControl() {
        return (StartNode) super.getControl();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getValue(Context context) {
        StartToken input = context.get(getControl());
        return (V) input.getArgument(this.index);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    public String label() {
        return "<param:" + this.index + "> " + getTypeDescriptor().label();
    }

    private final int index;
}
