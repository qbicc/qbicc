package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.InvokeValue;
import cc.quarkus.qcc.interpret.Context;

public class ThrowControlProjection extends AbstractNode<ControlToken> {

    protected ThrowControlProjection(InvokeNode<?> control) {
        super(control, ControlToken.class);
    }

    @Override
    public InvokeNode<?> getControl() {
        return (InvokeNode<?>) super.getControl();
    }

    @Override
    public ControlToken getValue(Context context) {
        InvokeValue input = context.get(getControl());
        if ( input.getThrowValue() != null ) {
            return new ControlToken();
        }
        return null;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }
}
