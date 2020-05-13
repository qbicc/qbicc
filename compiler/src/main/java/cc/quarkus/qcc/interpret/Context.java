package cc.quarkus.qcc.interpret;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.type.QType;

public interface Context {
    <V extends QType> void set(Node<V> node, V value);
    <V extends QType> V get(Node<V> node);
    InterpreterThread thread();
}
