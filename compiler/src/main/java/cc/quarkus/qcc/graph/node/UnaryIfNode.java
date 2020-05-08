package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.interpret.Context;

public class UnaryIfNode extends IfNode {

    public UnaryIfNode(ControlNode<?> control, CompareOp op) {
        super(control, op);
    }

    @Override
    public IfToken getValue(Context context) {
        return null;
    }

    public void setInput(Node<?> input) {
        this.input = input;
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add(getControl());
            add(input);
        }};
    }

    @Override
    public String toString() {
        return "<if:" + getId() + "> " + getOp();
    }

    private Node<?> input;
}
