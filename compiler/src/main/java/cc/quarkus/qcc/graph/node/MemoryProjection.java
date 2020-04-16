package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.MemorySource;
import cc.quarkus.qcc.graph.type.MemoryType;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class MemoryProjection extends Projection<ControlNode<?>, MemoryType> {

    protected MemoryProjection(ControlNode<?> input) {
        super(input, MemoryType.INSTANCE);
    }

    @Override
    public String label() {
        return "<proj> memory";
    }

    @Override
    public Value<?> getValue(Context context) {
        MemorySource input = (MemorySource) context.get(getPredecessors().get(0));
        return input.getMemory();
    }
}
