package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;

public class BinaryIfNode<V extends QType> extends IfNode {

    public BinaryIfNode(Graph<?> graph, ControlNode<?> control, CompareOp op) {
        super(graph, control, op);
    }

    public void setLHS(Node<V> lhs) {
        this.lhs = lhs;
        lhs.addSuccessor(this);
    }

    public void setRHS(Node<V> rhs) {
        this.rhs = rhs;
        rhs.addSuccessor(this);
    }

    public V getLHSValue(Context context) {
        return this.lhs.getValue(context);
    }

    public V getRHSValue(Context context) {
        return this.rhs.getValue(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IfToken getValue(Context context) {
        V lhsValue = getLHSValue(context);
        V rhsValue = getRHSValue(context);
        switch (getOp()) {
            case EQUAL:
                return IfToken.of(lhsValue.equals(rhsValue));
            case NOT_EQUAL:
                return IfToken.of(lhsValue.equals(rhsValue));

        }

        if (lhsValue instanceof Comparable && rhsValue instanceof Comparable) {
            switch (getOp()) {
                case LESS_THAN:
                    return IfToken.of(((Comparable) lhsValue).compareTo(rhsValue) < 0);
                case LESS_THAN_OR_EQUAL:
                    return IfToken.of(((Comparable) lhsValue).compareTo(rhsValue) <= 0);
                case GREATER_THAN:
                    return IfToken.of(((Comparable) lhsValue).compareTo(rhsValue) > 0);
                case GREATER_THAN_OR_EQUAL:
                    return IfToken.of(((Comparable) lhsValue).compareTo(rhsValue) >= 0);
            }
        }

        throw new UnsupportedOperationException(lhsValue + " " + getOp() + " " + rhsValue);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        if (lhs == null) {
            return List.of(getControl());
        }
        return List.of(getControl(), lhs, rhs);
    }

    private Node<V> lhs;

    private Node<V> rhs;


}
