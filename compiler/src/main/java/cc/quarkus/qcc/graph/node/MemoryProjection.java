package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.MemoryType;

public class MemoryProjection extends Projection<ControlNode<?>, MemoryType> {

    protected MemoryProjection(ControlNode<?> input) {
        super(input, MemoryType.INSTANCE);
    }

    @Override
    public String label() {
        return "<proj> memory";
    }
}
