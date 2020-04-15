package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.graph.type.Value;

public class VariableProjection<T extends ConcreteType<?>> extends Projection<StartNode, T> {

    protected VariableProjection(StartNode in, T outType, int index) {
        super(in, outType);
        this.index = index;
    }

    public String label() {
        return "<param> " + index + ": " + getType().label();
    }


    private final int index;
}
