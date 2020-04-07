package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.ControlType;

import static cc.quarkus.qcc.parse.TypeUtil.checkType;

public class EndNode<T extends ConcreteType<?>> extends ControlNode<ControlType> {
    public EndNode(T returnType) {
        super(ControlType.INSTANCE, 0, 0);
        this.returnType = returnType;
    }

    public T getReturnType() {
        return this.returnType;
    }

    public void returnValue(Node<? extends ConcreteType<?>> val) {
        addPredecessor(val);
        frame().returnValue(checkType(val, getReturnType()));
    }

    @Override
    public String label() {
        return "<end>";
    }

    private final T returnType;
}
