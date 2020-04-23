package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class NewNode<V> extends AbstractNode<V> {
    public NewNode(ControlNode<?> control, TypeDescriptor<V> typeDescriptor) {
        super(control, typeDescriptor);
        this.typeDescriptor = typeDescriptor;
    }

    @Override
    public V getValue(Context context) {
        //return null;
        throw new RuntimeException("not implemented");
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<new> " + getType();
    }

    private final TypeDescriptor<V> typeDescriptor;
}
