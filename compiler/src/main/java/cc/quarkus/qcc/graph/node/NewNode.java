package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.ObjectType;

public class NewNode<T extends ConcreteType<?>> extends Node<T> {
    public NewNode(ControlNode<?> control, T outType) {
        super(control, outType);
    }

    @Override
    public String label() {
        return "<new> " + getType();
    }
}
