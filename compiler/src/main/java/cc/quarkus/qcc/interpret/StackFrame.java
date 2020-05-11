package cc.quarkus.qcc.interpret;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.Node;

public class StackFrame implements Context {

    protected StackFrame(Heap heap) {
        this.heap = heap;
    }

    @Override
    public <T> void set(Node<T> node, T value) {
        this.bindings.put(node, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Node<T> node) {
        return (T) this.bindings.get(node);
    }

    @Override
    public Heap heap() {
        return this.heap;
    }

    public boolean contains(Node<?> node) {
        return this.bindings.containsKey(node);
    }

    private final Heap heap;

    private Map<Node<?>, Object> bindings = new HashMap<>();
}
