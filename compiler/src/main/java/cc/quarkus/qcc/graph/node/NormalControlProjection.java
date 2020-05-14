package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public class NormalControlProjection extends AbstractControlNode<ControlToken> implements Projection {

    protected NormalControlProjection(Graph<?> graph, InvokeNode<?> in) {
        super(graph, in, EphemeralTypeDescriptor.CONTROL_TOKEN);
    }

    @Override
    public InvokeNode<?> getControl() {
        return (InvokeNode<?>) super.getControl();
    }

    @Override
    public ControlToken getValue(Context context) {
        InvokeToken input = context.get(getControl());
        if ( input.getThrowValue() == null ) {
            return ControlToken.CONTROL;
        }
        return null;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl());
    }

    @Override
    public String label() {
        return "<proj:" + getId() + "> normal control";
    }

    @Override
    public String toString() {
        return label();
    }
}
