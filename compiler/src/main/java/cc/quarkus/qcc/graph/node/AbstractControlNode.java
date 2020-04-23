package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.parse.Frame;
import cc.quarkus.qcc.type.TypeDescriptor;

public abstract class AbstractControlNode<V> extends AbstractNode<V> implements ControlNode<V> {

    protected AbstractControlNode(ControlNode<?> control, TypeDescriptor<V> outType) {
        super(control, outType);
        this.frame = new Frame(this, control.frame().maxLocals(), control.frame().maxStack());
    }

    public AbstractControlNode(TypeDescriptor<V> outType, int maxLocals, int maxStack) {
        super(outType);
        this.frame = new Frame(this, maxLocals, maxStack);
    }

    @Override
    public void mergeInputs() {
        frame().mergeInputs();
        for (ControlNode<?> each : getControlSuccessors()) {
            if (each instanceof Projection) {
                //System.err.println( "** merge from " + this + " to " + each);
                each.mergeInputs();
            }
        }
    }

    @Override
    public Frame frame() {
        return this.frame;
    }

    private Frame frame;
}
