package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.interpret.Context;

public class IfTrueProjection extends AbstractControlNode<ControlToken> {

    protected IfTrueProjection(IfNode in) {
        super(in, ControlToken.class);
    }

    @Override
    public IfNode getControl() {
        return (IfNode) super.getControl();
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> true";
    }

    @Override
    public ControlToken getValue(Context context) {
        IfValue input = context.get(getControl());
        if ( input.getValue() ) {
            return new ControlToken();
        }
        return null;
    }

}
