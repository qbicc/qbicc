package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class VariableProjection<T extends ConcreteType<T>, V extends Value<T,V>> extends AbstractNode<T,V> {

    protected VariableProjection(StartNode in, T outType, int index) {
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
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    public String label() {
        return "<param> " + index + ": " + getType().label();
    }

    /*
    @Override
    public Value<?> getValue(Context context) {
        StartValue input = (StartValue) context.get(getPredecessors().get(0));
        return input.getArgument(this.index);
    }


     */
    private final int index;
}
