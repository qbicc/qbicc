package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class IfFalseProjection extends AbstractControlNode<ControlToken> implements Projection {

    protected IfFalseProjection(Graph<?> graph, IfNode in) {
        super(graph, in, TypeDescriptor.EphemeralTypeDescriptor.CONTROL_TOKEN);
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
        return "<proj> false";
    }

    @Override
    public ControlToken getValue(Context context) {
        IfToken input = context.get(getControl());
        if ( ! input.getValue() ) {
            return ControlToken.CONTROL;
        }
        return ControlToken.NO_CONTROL;
    }

}
