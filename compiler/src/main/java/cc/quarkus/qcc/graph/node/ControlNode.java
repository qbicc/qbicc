package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ControlType;
import cc.quarkus.qcc.parse.Frame;

public abstract class ControlNode<T extends ControlType> extends Node<T> {
    protected ControlNode(ControlNode<?> control, T outType) {
        super(control, outType);
        this.frame = new Frame(this, control.frame().maxLocals(), control.frame().maxStack());
    }

    public ControlNode(T outType, int maxLocals, int maxStack) {
        super(outType);
        this.frame = new Frame(this, maxLocals, maxStack);
    }

    public <T extends ControlNode<?>> void addInput(T node) {
        System.err.println( this + " add input " + node );
        if ( node != this && ! this.getPredecessors().contains(node)) {
            addPredecessor(node);
            frame().merge(node.frame());
        }
    }

    public Frame frame() {
        return this.frame;
    }

    protected void setFrame(Frame frame) {
        this.frame = frame;
    }

    public void possiblySimplify() {
        this.frame.possiblySimplify();
    }

    private Frame frame;
}
