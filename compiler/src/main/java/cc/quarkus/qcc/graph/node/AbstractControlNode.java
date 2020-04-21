package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.parse.Frame;

public abstract class AbstractControlNode<V> extends AbstractNode<V> implements ControlNode<V> {

    protected AbstractControlNode(ControlNode<?> control, Class<V> outType) {
        super(control, outType);
        this.frame = new Frame(this, control.frame().maxLocals(), control.frame().maxStack());
    }

    public AbstractControlNode(Class<V> outType, int maxLocals, int maxStack) {
        super(outType);
        this.frame = new Frame(this, maxLocals, maxStack);
    }

    @Override
    public Frame frame() {
        return this.frame;
    }

    private Frame frame;
}
