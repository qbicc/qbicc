package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.parse.Frame;

public interface ControlNode<T extends Type<T>, V extends Value<T,V>> extends Node<T, V> {
    Frame frame();
}
