package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class ThrowNode<T extends ConcreteType<?>> extends Node<T> {
    public ThrowNode(ControlNode<?> control, Node<T> thrown) {
        super(control, thrown.getType());
        addPredecessor(thrown);
    }

    @Override
    public String label() {
        return "<throw> " + getType();
    }
}
