package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.IOType;

public class IOProjection extends Projection<ControlNode<?>, IOType> {

    protected IOProjection(ControlNode<?> control) {
        super(control, IOType.INSTANCE);
    }

    @Override
    public String label() {
        return "<proj> i/o";
    }
}
