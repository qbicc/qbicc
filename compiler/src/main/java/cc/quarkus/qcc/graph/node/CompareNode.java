package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.interpret.Context;

public class CompareNode<V extends Comparable<V>> extends BinaryNode<V, Boolean> {

    protected CompareNode(ControlNode<?> control, Node<V> lhs, Node<V> rhs, CompareOp op) {
        super(control, Boolean.class);
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
