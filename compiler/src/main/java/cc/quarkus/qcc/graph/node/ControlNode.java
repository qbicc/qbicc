package cc.quarkus.qcc.graph.node;

import java.util.Set;

import cc.quarkus.qcc.graph.build.Frame;

public interface ControlNode<V> extends Node<V> {
    Frame frame();
    void mergeInputs();
    default void removeUnreachable(Set<ControlNode<?>> reachable) {

    }
}
