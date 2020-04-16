package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.CompareOp;

public interface Value<T extends Type<?>> {
    T getType();
    default boolean compare(CompareOp op, Value<T> other) {
        return false;
    }
}
