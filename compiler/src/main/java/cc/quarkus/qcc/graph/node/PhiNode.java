package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.build.PhiData;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.graph.build.GraphBuilder;
import cc.quarkus.qcc.graph.build.PhiLocal;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class PhiNode<V extends QType> extends AbstractNode<V> {

    public PhiNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType, PhiData data) {
        super(graph, control, outType);
        this.data = data;
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
        if ( this.data instanceof PhiLocal ) {
            if (((PhiLocal)this.data).getIndex() == GraphBuilder.SLOT_COMPLETION) {
                return "<phi:" + getId() + "> completion";
            } else if (((PhiLocal)this.data).getIndex() == GraphBuilder.SLOT_IO) {
                return "<phi:" + getId() + "> i/o";
            } else if (((PhiLocal)this.data).getIndex() == GraphBuilder.SLOT_MEMORY) {
                return "<phi:" + getId() + "> memory";
            }
        }
        return "<phi:" + getId() + "> " + getTypeDescriptor().label();
    }

    @Override
    public String toString() {
        return label();
    }

    @SuppressWarnings("unchecked")
    public Node<V> getValue(ControlNode<?> discriminator) {
        return (Node<V>) this.data.getValue(discriminator);
    }

    public void addInput(Node<?> input) {
        if ( input == null ) {
            return;
        }
        this.inputs.add(input);
        input.addSuccessor(this);
    }

    public final List<Node<?>> inputs = new ArrayList<>();

    private final PhiData data;

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int id;

}
