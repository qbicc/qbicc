package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.EndType;

public class EndNode<T extends ConcreteType<?>> extends Node<EndType<T>> {
    public EndNode(ControlNode<?> control, T returnType) {
        super(control, new EndType<>(returnType));
    }

    public String label() {
        return "<end>";
    }

}
