package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.MemorySource;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class MemoryProjection extends AbstractNode<MemoryToken> {

    protected <T extends ControlNode<? extends MemorySource>> MemoryProjection(T input) {
        super(input, TypeDescriptor.EphemeralTypeDescriptor.MEMORY_TOKEN);
    }

    @SuppressWarnings("unchecked")
    public ControlNode<? extends MemorySource> getControl() {
        return (ControlNode<? extends MemorySource>) super.getControl();
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<proj> memory";
    }

    @Override
    public MemoryToken getValue(Context context) {
        MemorySource input = context.get(getControl());
        return input.getMemory();
    }
}
