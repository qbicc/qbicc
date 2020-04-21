package cc.quarkus.qcc.interpret;

import cc.quarkus.qcc.graph.node.Node;

public interface Context {
    <V> void set(Node<V> node, V value);
    <V> V get(Node<V> node);
}
