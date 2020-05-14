package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.IfToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public class IfTrueProjection extends AbstractControlNode<ControlToken> implements Projection {

    protected IfTrueProjection(Graph<?> graph, IfNode in) {
        super(graph, in, EphemeralTypeDescriptor.CONTROL_TOKEN);
    }

    @Override
    public IfNode getControl() {
        return (IfNode) super.getControl();
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl());
    }

    @Override
    public String label() {
        return "<proj> true";
    }

    @Override
    public ControlToken getValue(Context context) {
        IfToken input = context.get(getControl());
        if (input.getValue()) {
            return ControlToken.CONTROL;
        }
        return ControlToken.NO_CONTROL;
    }

}
