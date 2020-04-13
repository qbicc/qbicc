package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class BinaryIfNode<T extends ConcreteType<?>> extends IfNode {

    public BinaryIfNode(ControlNode<?> control, CompareOp op) {
        super(control, op);
    }

}
