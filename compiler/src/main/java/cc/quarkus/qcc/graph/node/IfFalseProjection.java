package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ControlType;

public class IfFalseProjection extends ControlProjection<IfNode, ControlType> {

    protected IfFalseProjection(IfNode in) {
        super(in, ControlType.INSTANCE);
    }
}
