package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.parse.BytecodeParser;
import cc.quarkus.qcc.parse.PhiLocal;
import cc.quarkus.qcc.type.TypeDescriptor;

public class PhiNode<V> extends AbstractNode<V> {

    public PhiNode(ControlNode<?> control, TypeDescriptor<V> outType, PhiLocal local) {
        super(control, outType);
        this.local = local;
        this.id = COUNTER.incrementAndGet();
    }

    @Override
    public V getValue(Context context) {
        throw new UnsupportedOperationException("Phi has no value without discriminator");
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        if (this.local.getIndex() == BytecodeParser.SLOT_RETURN) {
            return "<phi> return";
        } else if (this.local.getIndex() == BytecodeParser.SLOT_IO) {
            return "<phi> i/o";
        } else if (this.local.getIndex() == BytecodeParser.SLOT_MEMORY) {
            return "<phi> memory";
        }
        return "<phi> " + getTypeDescriptor().label();
    }

    //@Override
    //public String toString() {
        //return label();
    //}

    public Node<V> getValue(ControlNode<?> discriminator) {
        return (Node<V>) this.local.getValue(discriminator);
    }

    public void addInput(Node<?> input) {
        this.inputs.add(input);
        input.addSuccessor(this);
    }

    private final List<Node<?>> inputs = new ArrayList<>();

    private final PhiLocal local;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;

}
