package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.Node;

public interface MemoryProvider {
    Node<MemoryToken> getMemory();
}
