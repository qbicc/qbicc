package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.graph.type.ControlValue;
import cc.quarkus.qcc.graph.type.InvokeValue;
import cc.quarkus.qcc.graph.type.ThrowType;
import cc.quarkus.qcc.interpret.Context;

public class ThrowControlProject extends AbstractNode<ControlType, ControlValue> {

    protected ThrowControlProject(InvokeNode control) {
        super(control, ControlType.INSTANCE);
    }

    @Override
    public InvokeNode getControl() {
        return (InvokeNode) super.getControl();
    }

    @Override
    public ControlValue getValue(Context context) {
        InvokeValue input = context.get(getControl());
        if ( input.getThrowValue() != null ) {
            return new ControlValue();
        }
        return null;
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }
}
