package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.graph.build.CatchMatcher;
import cc.quarkus.qcc.type.TypeDescriptor;

public class CatchControlProjection extends AbstractControlNode<ObjectReference> implements Projection {

    public CatchControlProjection(ThrowControlProjection control, ExceptionProjection exception, CatchMatcher matcher) {
        super(control, TypeDescriptor.OBJECT);
        this.exception = exception;
        this.matcher = matcher;
    }

    public ExceptionProjection getException() {
        return this.exception;
    }

    public ExceptionCompletionNode getCompletionOut() {
        return this.completionOut.updateAndGet(cur -> Objects.requireNonNullElseGet(cur, () -> new ExceptionCompletionNode(this, this.exception)));
    }

    @Override
    public ThrowControlProjection getControl() {
        return (ThrowControlProjection) super.getControl();
    }

    @Override
    public ObjectReference getValue(Context context) {
        InvokeToken input = context.get(getControl());
        if ( input.getThrowValue() == null ) {
            return null;
        }
        if ( ! this.matcher.matches(input.getThrowValue().getTypeDefinition())) {
            return null;
        }
        return input.getThrowValue();
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
