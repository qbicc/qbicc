package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class BinaryIfNode<T extends ConcreteType<?>> extends IfNode {
    public BinaryIfNode(ControlNode<?> control, Node<T> lhs, Node<T> rhs, CompareOp op) {
        super(control, op);
        addPredecessor(lhs);
        addPredecessor(rhs);
    }

    @Override
    public <T extends ControlNode<?>> void addInput(T node) {
        super.addInput(node);
        getTrueOut().frame().merge(node.frame());
        getFalseOut().frame().merge(node.frame());
    }
}
