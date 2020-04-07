package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.IntType;

public class UnaryIfNode extends IfNode {

    public UnaryIfNode(ControlNode<?> control, Node<IntType> test, CompareOp op) {
        super(control, op);
        addPredecessor(test);
    }
}
