package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;

public class ThrowNode<V extends ObjectReference> extends AbstractNode<V> {

    public ThrowNode(ControlNode<?> control, Node<V> thrown) {
        super(control, thrown.getTypeDescriptor());
        this.thrown = thrown;
    }

    @Override
    public V getValue(Context context) {
        return context.get(getThrown());
    }

    public Node<V> getThrown() {
        return this.thrown;
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl() );
            add( getThrown() );
        }};
    }

    @Override
    public String label() {
        return "<throw> " + getType();
    }

    private final Node<V> thrown;
}
