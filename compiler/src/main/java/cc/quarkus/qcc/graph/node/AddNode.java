package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.Type;

public class AddNode<T extends Type<?>> extends BinaryNode<T> {

    public AddNode(ControlNode<?> control, T outType, Node<T> lhs, Node<T> rhs) {
        super(control, outType);
        addPredecessor(lhs);
        addPredecessor(rhs);
    }
}
