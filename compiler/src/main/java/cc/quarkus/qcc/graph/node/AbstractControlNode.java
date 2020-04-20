package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.parse.Frame;

public abstract class AbstractControlNode<T extends Type<T>, V extends Value<T,V>> extends AbstractNode<T,V> implements ControlNode<T,V> {

    protected AbstractControlNode(ControlNode<?,?> control, T outType) {
        super(control, outType);
        this.frame = new Frame(this, control.frame().maxLocals(), control.frame().maxStack());
    }

    public AbstractControlNode(T outType, int maxLocals, int maxStack) {
        super(outType);
        this.frame = new Frame(this, maxLocals, maxStack);
    }
    /*

    public AbstractControlNode(T outType) {
        super(outType);
    }

    public <T extends AbstractControlNode<?>> void addInput(T node) {
        if ( node != this && ! this.getPredecessors().contains(node)) {
            addPredecessor(node);
        }
    }
     */

    @Override
    public Frame frame() {
        return this.frame;
    }

    private Frame frame;
}
