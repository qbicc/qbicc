package cc.quarkus.qcc.interpret;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.Value;

public class ThreadContext implements Context {

    @Override
    public void set(Node<?> node, Value<?> value) {
        this.bindings.put(node, value);
    }

    @Override
    public Value<?> get(Node<?> node) {
        return this.bindings.get(node);
    }

    private Map<Node<?>, Value<?>> bindings = new HashMap<>();
}
