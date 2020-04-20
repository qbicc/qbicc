package cc.quarkus.qcc.interpret;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

public interface Context {
    <T extends Type<T>, V extends Value<T,V>> void set(Node<T,V> node, V value);
    <T extends Type<T>, V extends Value<T,V>> V get(Node<T,V> node);
}
