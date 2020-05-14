package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ControlToken;
import cc.quarkus.qcc.graph.type.ExceptionProvider;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.graph.build.CatchMatcher;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public class CatchControlProjection extends AbstractControlNode<ControlToken> implements Projection, ExceptionProvider {

    public CatchControlProjection(Graph<?> graph, ThrowControlProjection control, ExceptionProjection exception, CatchMatcher matcher) {
        super(graph, control, EphemeralTypeDescriptor.CONTROL_TOKEN);
        this.exception = exception;
        this.matcher = matcher;
    }

    @Override
    public ExceptionProjection getException() {
        return this.exception;
    }

    public ExceptionCompletionNode getCompletionOut() {
        return this.completionOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ExceptionCompletionNode(getGraph(), this, this.exception)));
    }

    @Override
    public ControlToken getValue(Context context) {
        //ObjectReference input = context.get(this.exception);
        ObjectReference input = this.exception.getValue(context);
        if ( this.matcher.matches(input.getTypeDefinition())) {
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
        return "<proj:" + getId() + "> catch " + this.matcher;
    }

    @Override
    public String toString() {
        return label();
    }

    private final CatchMatcher matcher;

    private final ExceptionProjection exception;

    private AtomicReference<ExceptionCompletionNode> completionOut = new AtomicReference<>();
}
