package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class ThrowNode<T extends ConcreteType<T>, V extends Value<T,V>> extends AbstractNode<T,V> {

    public ThrowNode(ControlNode<?,?> control, Node<T,V> thrown) {
        super(control, thrown.getType());
        this.thrown = thrown;
    }

    @Override
    public V getValue(Context context) {
        return context.get(getThrown());
    }

    public Node<T, V> getThrown() {
        return this.thrown;
    }

    @Override
    public List<? extends Node<?, ?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl() );
            add( getThrown() );
        }};
    }

    @Override
    public String label() {
        return "<throw> " + getType();
    }

    private final Node<T, V> thrown;
}
