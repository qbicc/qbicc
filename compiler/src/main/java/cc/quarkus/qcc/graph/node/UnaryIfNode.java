package cc.quarkus.qcc.graph.node;

public class UnaryIfNode extends IfNode {

    /*
    public UnaryIfNode(ControlNode<?> control, Node<IntType> test, CompareOp op) {
        super(control, op);
        addPredecessor(test);
    }
     */

    public UnaryIfNode(AbstractControlNode<?> control, CompareOp op) {
        super(control, op);
    }
}
