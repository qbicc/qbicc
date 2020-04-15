package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ControlType;

public class ControlProjection<INPUT extends ControlNode<?>, OUTPUT extends ControlType> extends ControlNode<OUTPUT> {
    protected ControlProjection(INPUT in, OUTPUT outType) {
        super(in, outType);
    }
}
