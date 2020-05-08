package cc.quarkus.qcc.graph.node;

import java.util.function.BiFunction;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class SubNode<V extends Number> extends BinaryNode<V,V> {

    public SubNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType, Node<V> lhs, Node<V> rhs, BiFunction<V,V,V> subber) {
        super(graph, control, outType);
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
