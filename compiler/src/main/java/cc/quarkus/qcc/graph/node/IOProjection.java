package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.IOType;

public class IOProjection extends Node<IOType> {
    protected IOProjection(ControlNode<?> control) {
        super(control, IOType.INSTANCE);
    }
}
