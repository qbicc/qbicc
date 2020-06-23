package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.graph.Type;

public interface FieldDescriptor {
    String getName();
    Type getType();
}
