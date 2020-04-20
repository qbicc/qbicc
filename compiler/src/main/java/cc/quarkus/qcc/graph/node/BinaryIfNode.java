package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.graph.type.NumericType;
import cc.quarkus.qcc.graph.type.NumericValue;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

import static cc.quarkus.qcc.graph.type.IfValue.of;

public class BinaryIfNode<T extends Type<T>, V extends Value<T,V>> extends IfNode {

    public BinaryIfNode(ControlNode<?,?> control, CompareOp op) {
        super(control, op);
    }


    public void setLHS(Node<T,V> lhs) {
        this.lhs = lhs;
        lhs.addSuccessor(this);
    }
    public void setRHS(Node<T,V> rhs) {
        this.rhs = rhs;
        rhs.addSuccessor(this);
    }

    public V getLHSValue(Context context ) {
        return this.lhs.getValue(context);
    }

    public V getRHSValue(Context context) {
        return this.rhs.getValue(context);
    }

    private Node<T, V> lhs;
    private Node<T, V> rhs;


    @Override
    public IfValue getValue(Context context) {
        V lhsValue = getLHSValue(context);
        V rhsValue = getLHSValue(context);
        switch (getOp()) {
            case EQUAL:
                return IfValue.of(lhsValue.equals(rhsValue));
            case NOT_EQUAL:
                return IfValue.of(lhsValue.equals(rhsValue));
        }

        if ( lhsValue instanceof NumericValue && rhsValue instanceof NumericValue ) {
            return IfValue.of(getOp().execute( lhsValue, rhsValue));
        }

        throw new UnsupportedOperationException( lhsValue + " " + getOp() + " " + rhsValue);
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return null;
    }


}
