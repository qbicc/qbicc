package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.type.ObjectReference;

public interface ExceptionProvider {
    Node<ObjectReference> getException();
}
