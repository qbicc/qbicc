package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.MemorySource;
import cc.quarkus.qcc.graph.type.MemoryType;
import cc.quarkus.qcc.graph.type.MemoryValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class MemoryProjection extends AbstractNode<MemoryType, MemoryValue> {

    protected <T extends ControlNode<?,? extends MemorySource>> MemoryProjection(T input) {
        super(input, MemoryType.INSTANCE);
    }

    @SuppressWarnings("unchecked")
    public ControlNode<?,? extends MemorySource> getControl() {
        return (ControlNode<?, ? extends MemorySource>) super.getControl();
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> memory";
    }

    @Override
    public MemoryValue getValue(Context context) {
        MemorySource input = context.get(getControl());
        return input.getMemory();
    }
}
