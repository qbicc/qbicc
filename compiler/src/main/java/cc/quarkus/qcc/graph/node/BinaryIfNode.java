package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.interpret.Context;

public class BinaryIfNode<V> extends IfNode {

    public BinaryIfNode(ControlNode<?> control, CompareOp op) {
        super(control, op);
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

    @Override
    public IfValue getValue(Context context) {
        V lhsValue = getLHSValue(context);
        V rhsValue = getRHSValue(context);
        switch (getOp()) {
            case EQUAL:
                return IfValue.of(lhsValue.equals(rhsValue));
            case NOT_EQUAL:
                return IfValue.of(lhsValue.equals(rhsValue));

        }
        
        if ( lhsValue instanceof Comparable && rhsValue instanceof Comparable ) {
            System.err.println( lhsValue + " " + getOp() + " " + rhsValue);
            switch ( getOp() ) {
                case LESS_THAN:
                    return IfValue.of(((Comparable) lhsValue).compareTo(rhsValue) < 0);
                case LESS_THAN_OR_EQUAL:
                    return IfValue.of(((Comparable) lhsValue).compareTo(rhsValue) <= 0);
                case GREATER_THAN:
                    return IfValue.of(((Comparable) lhsValue).compareTo(rhsValue) > 0);
                case GREATER_THAN_OR_EQUAL:
                    return IfValue.of(((Comparable) lhsValue).compareTo(rhsValue) >= 0);
            }
        }

        throw new UnsupportedOperationException(lhsValue + " " + getOp() + " " + rhsValue);
    }

    @Override
    public List<Node<?>> getPredecessors() {
       return new ArrayList<>() {{
            add(getControl());
            if ( lhs != null ) {
                add(lhs);
            }
            if ( rhs != null ) {
                add(rhs);
            }
        }};
    }

    private Node<V> lhs;

    private Node<V> rhs;


}
