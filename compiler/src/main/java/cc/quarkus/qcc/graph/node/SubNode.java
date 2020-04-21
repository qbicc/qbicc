package cc.quarkus.qcc.graph.node;

import java.util.function.BiFunction;

import cc.quarkus.qcc.interpret.Context;

public class SubNode<V extends Number> extends BinaryNode<V,V> {

    public SubNode(ControlNode<?> control, Class<V> outType, Node<V> lhs, Node<V> rhs, BiFunction<V,V,V> subber) {
        super(control, outType);
        setLHS(lhs);
        setRHS(rhs);
        this.subber = subber;
    }

    @Override
    public V getValue(Context context) {
        V lhsValue = getLHSValue(context);
        V rhsValue = getRHSValue(context);
        return this.subber.apply(lhsValue, rhsValue);
    }

    private final BiFunction<V, V, V> subber;
}
