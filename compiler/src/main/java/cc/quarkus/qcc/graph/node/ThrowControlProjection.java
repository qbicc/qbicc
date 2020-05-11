package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.ThrowSource;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ThrowControlProjection extends AbstractControlNode<ControlToken> implements Projection {

    protected ThrowControlProjection(Graph<?> graph, ControlNode<?> control) {
        super(graph, control, TypeDescriptor.EphemeralTypeDescriptor.CONTROL_TOKEN);
    }

    @SuppressWarnings("unchecked")
    public ControlNode<? extends ThrowSource> getThrowSource() {
        return (ControlNode<? extends ThrowSource>) getControl();
    }

    @Override
    public ControlToken getValue(Context context) {
        ThrowSource input = getThrowSource().getValue(context);
        if ( input.getThrowValue() != null ) {
            return ControlToken.CONTROL;
        }
        return ControlToken.NO_CONTROL;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj:" + getId() + "> throw control";
    }

    @Override
    public String toString() {
        return label();
    }
}
