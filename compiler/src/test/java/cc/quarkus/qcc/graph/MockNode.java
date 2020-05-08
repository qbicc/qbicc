package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class MockNode<V> extends AbstractNode<V> {

    MockNode(ControlNode<?> control, TypeDescriptor<V> type, V value) {
        super(control, type);
        this.value = value;
    }

    @Override
    public V getValue(Context context) {
        return this.value;
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return null;
    }

    private final V value;
}
