package cc.quarkus.qcc.interpret;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.Value;

public interface Context {
    void set(Node<?> node, Value<?> value);
    Value<?> get(Node<?> node);
}
