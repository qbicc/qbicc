package cc.quarkus.qcc.graph;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.interpret.Heap;

public class MockContext implements Context  {

    MockContext(Heap heap) {
        this.heap = heap;
    }

    @Override
    public <V> void set(Node<V> node, V value) {
        this.values.put(node, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V get(Node<V> node) {
        return (V) this.values.get(node);
    }

    @Override
    public Heap heap() {
        return this.heap;
    }

    private final Heap heap;
    private Map<Node<?>, Object> values = new HashMap<>();
}
