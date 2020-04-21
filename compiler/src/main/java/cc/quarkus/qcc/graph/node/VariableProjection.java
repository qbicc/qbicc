package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.interpret.Context;

public class VariableProjection<V> extends AbstractNode<V> {

    protected VariableProjection(StartNode in, Class<V> outType, int index) {
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
        StartValue input = context.get(getControl());
        return (V) input.getArgument(this.index);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    public String label() {
        return "<param> " + index + ": " + getType();
    }

    private final int index;
}
