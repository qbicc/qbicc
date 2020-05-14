package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.StartToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public class StartNode extends AbstractControlNode<StartToken> {

    public StartNode(Graph<?> graph) {
        super(graph, EphemeralTypeDescriptor.START_TOKEN);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.emptyList();
    }

    @Override
    public StartToken getValue(Context context) {
        return context.get(this);
    }

    @Override
    public String label() {
        return "<start:" + getId() + ">";
    }

}
