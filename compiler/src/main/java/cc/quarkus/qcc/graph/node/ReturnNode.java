package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.CompletionToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ReturnNode<V> extends AbstractNode<CompletionToken> {
    public ReturnNode(ControlNode<?> control, Node<V> input) {
        super(control, TypeDescriptor.EphemeralTypeDescriptor.COMPLETION_TOKEN);
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
        return new ArrayList<>() {{
            add( getControl() );
            add( getInput() );
        }};
    }

    @Override
    public String label() {
        return "<return-completion: " + getId()  + ">";
    }

    private final Node<V> input;
}
