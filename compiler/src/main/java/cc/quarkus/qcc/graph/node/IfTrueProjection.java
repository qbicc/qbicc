package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ControlType;

public class IfTrueProjection extends ControlProjection<IfNode, ControlType> {

    protected IfTrueProjection(IfNode in) {
        super(in, ControlType.INSTANCE);
    }

}
