package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.interpret.Context;

public class UnaryIfNode extends IfNode {

    public UnaryIfNode(Graph<?> graph, ControlNode<?> control, CompareOp op) {
        super(graph, control, op);
    }

    @Override
    public IfToken getValue(Context context) {
        Object val = context.get(this.test);
        switch ( getOp() ) {
            case EQUAL:
                break;
            case NOT_EQUAL:
                break;
            case LESS_THAN:
                break;
            case LESS_THAN_OR_EQUAL:
                break;
            case GREATER_THAN:
                break;
            case GREATER_THAN_OR_EQUAL:
                break;
            case NULL:
                return IfToken.of ( val == null );
            case NONNULL:
                return IfToken.of ( val != null );
        }
        return null;
    }

    public void setTest(Node<?> test) {
        this.test = test;
        test.addSuccessor(this);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        if ( this.test == null ) {
            return List.of(getControl());
        }
        return List.of(getControl(), this.test);
    }

    @Override
    public String toString() {
        return "<if:" + getId() + "> " + getOp();
    }

    private Node<?> test;
}
