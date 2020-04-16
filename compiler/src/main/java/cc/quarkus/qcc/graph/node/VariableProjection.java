package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class VariableProjection<T extends ConcreteType<?>> extends Projection<StartNode, T> {

    protected VariableProjection(StartNode in, T outType, int index) {
        super(in, outType);
        this.index = index;
    }

    public String label() {
        return "<param> " + index + ": " + getType().label();
    }

    @Override
    public Value<?> getValue(Context context) {
        StartValue input = (StartValue) context.get(getPredecessors().get(0));
        return input.getArgument(this.index);
    }

    private final int index;
}
