package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import javax.naming.ldap.Control;

import cc.quarkus.qcc.graph.type.IOSource;
import cc.quarkus.qcc.graph.type.IOType;
import cc.quarkus.qcc.graph.type.IOValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class IOProjection extends AbstractNode<IOType, IOValue> {

    protected <T extends ControlNode<?,? extends IOSource>> IOProjection(T control) {
        super(control, IOType.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ControlNode<?, ? extends IOSource> getControl() {
        return (ControlNode<?, ? extends IOSource>) super.getControl();
    }

    @Override
    public IOValue getValue(Context context) {
        IOSource input = context.get(getControl());
        return input.getIO();
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> i/o";
    }
}
