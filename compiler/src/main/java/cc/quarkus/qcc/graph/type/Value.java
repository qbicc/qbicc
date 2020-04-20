package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.CompareOp;

public interface Value<T extends Type<T>, V extends Value<T,V>> extends Comparable<V>{
    T getType();

    @Override
    default int compareTo(V o) {
        throw new UnsupportedOperationException("comparison not supported for " + getClass().getName());
    }
}
