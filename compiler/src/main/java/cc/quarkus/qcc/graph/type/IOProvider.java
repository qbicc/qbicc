package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.Node;

public interface IOProvider {
    Node<IOToken> getIO();
}
