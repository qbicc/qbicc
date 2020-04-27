package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ThrowControlProjection extends AbstractControlNode<InvokeToken> implements Projection {

    protected ThrowControlProjection(InvokeNode<?> control) {
        super(control, TypeDescriptor.EphemeralTypeDescriptor.INVOKE_TOKEN);
    }

    @Override
    public InvokeNode<?> getControl() {
        return (InvokeNode<?>) super.getControl();
    }

    @Override
    public InvokeToken getValue(Context context) {
        InvokeToken input = context.get(getControl());
        if ( input.getThrowValue() != null ) {
            return input;
        }
        return null;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> throw control";
    }
}
