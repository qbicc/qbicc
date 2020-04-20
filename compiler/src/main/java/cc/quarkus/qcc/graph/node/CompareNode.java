package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.graph.type.BooleanType;
import cc.quarkus.qcc.graph.type.BooleanValue;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

public class CompareNode<T extends Type<T>, V extends Value<T,V>> extends BinaryNode<T, V, BooleanType, BooleanValue> {

    protected CompareNode(ControlNode<?,?> control, Node<T,V> lhs, Node<T,V> rhs, CompareOp op) {
        super(control, BooleanType.INSTANCE);
        setLHS(lhs);
        setRHS(rhs);
        this.op = op;
    }

    private final CompareOp op;
}
