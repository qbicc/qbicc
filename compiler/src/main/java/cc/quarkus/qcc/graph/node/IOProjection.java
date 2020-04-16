package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.IOSource;
import cc.quarkus.qcc.graph.type.IOType;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class IOProjection extends Projection<ControlNode<?>, IOType> {

    protected IOProjection(ControlNode<?> control) {
        super(control, IOType.INSTANCE);
    }

    @Override
    public Value<?> getValue(Context context) {
        IOSource input = (IOSource) context.get(getPredecessors().get(0));
        return input.getIO();
    }

    @Override
    public String label() {
        return "<proj> i/o";
    }
}
