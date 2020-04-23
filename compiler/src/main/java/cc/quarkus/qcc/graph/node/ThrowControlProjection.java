package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ThrowControlProjection extends AbstractNode<ControlToken> {

    protected ThrowControlProjection(InvokeNode<?> control) {
        super(control, TypeDescriptor.EphemeralTypeDescriptor.CONTROL_TOKEN);
    }

    @Override
    public InvokeNode<?> getControl() {
        return (InvokeNode<?>) super.getControl();
    }

    @Override
    public ControlToken getValue(Context context) {
        InvokeToken input = context.get(getControl());
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
