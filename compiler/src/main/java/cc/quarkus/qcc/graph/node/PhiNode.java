package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.graph.build.GraphBuilder;
import cc.quarkus.qcc.graph.build.PhiLocal;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class PhiNode<V extends QType> extends AbstractNode<V> {

    public PhiNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType, PhiLocal local) {
        super(graph, control, outType);
        this.local = local;
        this.id = COUNTER.incrementAndGet();
    }

    @Override
    public V getValue(Context context) {
        //throw new UnsupportedOperationException("Phi has no value without discriminator");
        return context.get(this);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl());
    }

    @Override
    public String label() {
        if (this.local.getIndex() == GraphBuilder.SLOT_COMPLETION) {
            return "<phi:" + getId() + "> completion";
        } else if (this.local.getIndex() == GraphBuilder.SLOT_IO) {
            return "<phi:" + getId() + "> i/o";
        } else if (this.local.getIndex() == GraphBuilder.SLOT_MEMORY) {
            return "<phi:" + getId() + "> memory";
        }
        return "<phi:" + getId() + "> " + getTypeDescriptor().label();
    }

    @Override
    public String toString() {
        return label();
    }

    @SuppressWarnings("unchecked")
    public Node<V> getValue(ControlNode<?> discriminator) {
        return (Node<V>) this.local.getValue(discriminator);
    }

    public void addInput(Node<?> input) {
        if ( input == null ) {
            return;
        }
        this.inputs.add(input);
        input.addSuccessor(this);
    }

    private final List<Node<?>> inputs = new ArrayList<>();

    private final PhiLocal local;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;

}
