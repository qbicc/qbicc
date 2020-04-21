package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.interpret.Context;

public class UnaryIfNode extends IfNode {

    public UnaryIfNode(ControlNode<?> control, CompareOp op) {
        super(control, op);
    }

    @Override
    public IfValue getValue(Context context) {
        return null;
    }

    public void setInput(Node<Integer> input) {
        this.input = input;
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return null;
    }

    private Node<Integer> input;
}
