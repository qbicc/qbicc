package cc.quarkus.qcc.interpret;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.Node;

public class ThreadContext implements Context {

    @Override
    public <T> void set(Node<T> node, T value) {
        this.bindings.put(node, value);
    }

    @Override
    public <T> T get(Node<T> node) {
        return (T) this.bindings.get(node);
    }

    private Map<Node<?>, Object> bindings = new HashMap<>();
}
