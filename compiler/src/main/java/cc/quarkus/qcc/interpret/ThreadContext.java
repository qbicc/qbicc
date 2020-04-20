package cc.quarkus.qcc.interpret;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.AbstractNode;
import cc.quarkus.qcc.graph.type.Value;

public class ThreadContext implements Context {

    @Override
    public void set(AbstractNode<?> node, Value<?> value) {
        this.bindings.put(node, value);
    }

    @Override
    public Value<?> get(AbstractNode<?> node) {
        return this.bindings.get(node);
    }

    private Map<AbstractNode<?>, Value<?>> bindings = new HashMap<>();
}
