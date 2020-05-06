package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class ExceptionProjection extends AbstractNode<ObjectReference> implements Projection {

    protected ExceptionProjection(InvokeNode<?> control) {
        super(control, TypeDescriptor.THROWABLE);
    }

    @Override
    public ObjectReference getValue(Context context) {
        return null;
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }
}
