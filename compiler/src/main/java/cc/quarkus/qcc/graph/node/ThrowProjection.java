package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ThrowType;

public class ThrowProjection extends Node<ThrowType> {

    protected ThrowProjection(ControlNode<?> control) {
        super(control, ThrowType.INSTANCE);
    }
}
