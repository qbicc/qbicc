package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class ReturnNode<T extends ConcreteType<?>> extends Node<T> {
    public ReturnNode(ControlNode<?> control, T outType, Node<T> value) {
        super(control, outType);
        addPredecessor(value);
    }

    @Override
    public String label() {
        return getId() + ": <return> " + getType().label();
    }

    @Override
    public Value<?> getValue(Context context) {
        Value<?> input = context.get(getPredecessors().get(1));
        return input;
    }
}
