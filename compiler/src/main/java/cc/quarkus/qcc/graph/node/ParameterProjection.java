package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.StartToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class ParameterProjection<V extends QType> extends AbstractNode<V> implements Projection {

    public ParameterProjection(Graph<?> graph, StartNode in, TypeDescriptor<V> outType, int index) {
        super(graph, in, outType);
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
        return List.of(getControl());
    }

    public String label() {
        return "<param:" + getId() + "> [" + this.index + "] " + getTypeDescriptor().label();
    }

    @Override
    public String toString() {
        return label();
    }

    private final int index;
}
