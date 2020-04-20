package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.graph.type.ControlValue;
import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.interpret.Context;

public class IfTrueProjection extends AbstractControlNode<ControlType, ControlValue> {

    protected IfTrueProjection(IfNode in) {
        super(in, ControlType.INSTANCE);
    }

    @Override
    public IfNode getControl() {
        return (IfNode) super.getControl();
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> true";
    }

    @Override
    public ControlValue getValue(Context context) {
        IfValue input = context.get(getControl());
        if ( input.getValue() ) {
            return new ControlValue();
        }
        return null;
    }

}
