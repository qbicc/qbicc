package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

import static cc.quarkus.qcc.graph.type.IfValue.of;

public class BinaryIfNode<T extends ConcreteType<?>> extends IfNode {

    public BinaryIfNode(ControlNode<?> control, CompareOp op) {
        super(control, op);
    }

    @Override
    public IfValue getValue(Context context) {
        Value<T> lhsValue = (Value<T>) context.get(getPredecessors().get(1));
        Value<T> rhsValue = (Value<T>) context.get(getPredecessors().get(2));
        boolean result = lhsValue.compare(getOp(), rhsValue);
        return of(result);
    }
}
