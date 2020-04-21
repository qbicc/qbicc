package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.parse.Frame;

public interface ControlNode<V> extends Node<V> {
    Frame frame();
}
