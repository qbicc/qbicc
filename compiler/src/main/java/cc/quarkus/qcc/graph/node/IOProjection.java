package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.IOSource;
import cc.quarkus.qcc.graph.type.IOToken;
import cc.quarkus.qcc.interpret.Context;

public class IOProjection extends AbstractNode<IOToken> {

    protected <T extends ControlNode<? extends IOSource>> IOProjection(T control) {
        super(control, IOToken.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ControlNode<? extends IOSource> getControl() {
        return (ControlNode<? extends IOSource>) super.getControl();
    }

    @Override
    public IOToken getValue(Context context) {
        IOSource input = context.get(getControl());
        return input.getIO();
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> i/o";
    }
}
