package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.BooleanType;

public class CompareNode extends Node<BooleanType> {

    protected CompareNode(ControlNode<?> control, Node<?> lhs, Node<?> rhs, CompareOp op) {
        super(control, BooleanType.INSTANCE);
        addPredecessor(lhs);
        addPredecessor(rhs);
        this.op = op;
    }

    private final CompareOp op;
}
