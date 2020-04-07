package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.MemoryType;

public class MemoryProjection extends Node<MemoryType> {

    protected MemoryProjection(ControlNode<?> control) {
        super(control, MemoryType.INSTANCE);
    }
}
