package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class CompareNode<V extends Comparable<V>> extends BinaryNode<V, Boolean> {

    protected CompareNode(Graph<?> graph, ControlNode<?> control, Node<V> lhs, Node<V> rhs, CompareOp op) {
        super(graph, control, TypeDescriptor.BOOLEAN);
        setLHS(lhs);
        setRHS(rhs);
        this.op = op;
    }

    @Override
    public Boolean getValue(Context context) {
        return this.op.execute(getLHSValue(context), getRHSValue(context));
    }

    private final CompareOp op;
}
