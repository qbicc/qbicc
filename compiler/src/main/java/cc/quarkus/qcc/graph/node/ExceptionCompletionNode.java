package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.CompletionToken;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ExceptionCompletionNode extends AbstractNode<CompletionToken>  {

    protected ExceptionCompletionNode(Graph<?> graph, CatchControlProjection control, Node<ObjectReference> exception) {
        super(graph, control, TypeDescriptor.EphemeralTypeDescriptor.COMPLETION_TOKEN);
        this.exception = exception;
        exception.addSuccessor(this);
    }

    @Override
    public CatchControlProjection getControl() {
        return (CatchControlProjection) super.getControl();
    }

    @Override
    public CompletionToken<?> getValue(Context context) {
        return new CompletionToken<>(null, context.get(this.exception));
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl());
            add( exception );
        }};
    }

    @Override
    public String label() {
        return "<exception-completion:" + getId() + ">";
    }

    private final Node<ObjectReference> exception;
}
