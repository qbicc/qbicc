package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.ControlType;

public class NormalControlProjection extends ControlProjection<InvokeNode, ControlType>{
    protected NormalControlProjection(InvokeNode in) {
        super(in, ControlType.INSTANCE);
    }

    @Override
    public String label() {
        return "<proj> normal control";
    }
}
