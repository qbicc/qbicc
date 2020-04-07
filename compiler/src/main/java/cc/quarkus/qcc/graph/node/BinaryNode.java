package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.Type;

public abstract class BinaryNode<T extends Type> extends Node<T> {

    protected BinaryNode(ControlNode<?> control, T outType) {
        super(control, outType);
    }

}
