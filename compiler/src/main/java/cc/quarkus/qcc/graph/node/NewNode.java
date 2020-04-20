package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class NewNode<T extends ConcreteType<T>, V extends Value<T, V>> extends AbstractNode<T, V> {
    public NewNode(ControlNode<?, ?> control, T outType) {
        super(control, outType);
    }

    @Override
    public V getValue(Context context) {
        //return null;
        throw new RuntimeException("not implemented");
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<new> " + getType();
    }
}
