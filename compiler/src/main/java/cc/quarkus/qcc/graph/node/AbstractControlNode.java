package cc.quarkus.qcc.graph.node;

import java.util.ListIterator;
import java.util.Set;

import cc.quarkus.qcc.graph.build.Frame;
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
    public void removeUnreachable(Set<ControlNode<?>> reachable) {
        ListIterator<Node<?>> iter = this.successors.listIterator();
        while (iter.hasNext()) {
            Node<?> each = iter.next();
            if (each instanceof PhiNode) {
                if (each.getSuccessors().isEmpty()) {
                    iter.remove();
                }
            }
        }
    }

    @Override
    public void mergeInputs() {
        frame().mergeInputs();
        for (ControlNode<?> each : getControlSuccessors()) {
            if (each instanceof Projection) {
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
