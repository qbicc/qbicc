package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.CompletionToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public class ReturnNode<V extends QType> extends AbstractNode<CompletionToken> {
    public ReturnNode(Graph<?> graph, ControlNode<?> control, Node<V> input) {
        super(graph, control, EphemeralTypeDescriptor.COMPLETION_TOKEN);
        this.input = input;
        input.addSuccessor(this);
    }

    public Node<V> getInput() {
        return this.input;
    }

    @Override
    public CompletionToken<V> getValue(Context context) {
        return new CompletionToken<>(context.get(getInput()), null);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl(), getInput());
    }

    @Override
    public String label() {
        return "<return-completion: " + getId()  + ">";
    }

    private final Node<V> input;
}
