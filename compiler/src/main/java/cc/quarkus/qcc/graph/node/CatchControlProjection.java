package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.CatchToken;
import cc.quarkus.qcc.graph.type.InvokeToken;
import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.TypeDescriptor;

public class CatchControlProjection extends AbstractControlNode<ObjectReference> implements Projection {

    public CatchControlProjection(ThrowControlProjection control, TypeDefinition catchType) {
        super(control, TypeDescriptor.OBJECT);
        this.catchType = catchType;
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
        if ( ! this.catchType.isAssignableFrom(input.getThrowValue().getTypeDefinition()) ) {
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
        return "<proj> catch " + this.catchType.getName();
    }

    private final TypeDefinition catchType;
}
