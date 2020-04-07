package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class ReturnNode<T extends ConcreteType<?>> extends Node<T> {
    public ReturnNode(ControlNode<?> control, T outType, Node<T> value) {
        super(control, outType);
        addPredecessor(value);
    }
}
