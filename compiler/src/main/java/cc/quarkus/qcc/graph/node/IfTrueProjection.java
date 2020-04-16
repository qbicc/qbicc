package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.graph.type.ControlValue;
import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class IfTrueProjection extends ControlProjection<IfNode, ControlType> {

    protected IfTrueProjection(IfNode in) {
        super(in, ControlType.INSTANCE);
    }

    @Override
    public String label() {
        return "<proj> true";
    }

    @Override
    public Value<?> getValue(Context context) {
        IfValue input = (IfValue) context.get(getPredecessors().get(0));
        if ( input.getValue() ) {
            return new ControlValue();
        }
        return null;
    }
}
