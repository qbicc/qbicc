package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.Type;

public class WidenNode<T extends Type> extends Node<T> {

    public WidenNode(ControlNode<?> control, Node<?> input, T outType) {
        super(control, outType);
        addPredecessor(input);
    }
}
