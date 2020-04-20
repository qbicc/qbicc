package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.NumericType;
import cc.quarkus.qcc.graph.type.NumericValue;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.interpret.Context;

public class SubNode<T extends NumericType<T>, V extends NumericValue<T,V>> extends BinaryNode<T,V,T,V> {

    public SubNode(ControlNode<?,?> control, T outType, Node<T,V> lhs, Node<T,V> rhs) {
        super(control, outType);
        setLHS(lhs);
        setRHS(rhs);
    }

    @Override
    public V getValue(Context context) {
        V lhsValue = getLHSValue(context);
        V rhsValue = getRHSValue(context);
        return (V) lhsValue.sub(rhsValue);
    }
}
