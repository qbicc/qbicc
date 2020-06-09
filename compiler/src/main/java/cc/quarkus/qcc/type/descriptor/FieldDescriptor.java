package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.ObjectReference;

public interface FieldDescriptor<V> {
    Type getTypeDescriptor();
    V get(ObjectReference objRef);
    void put(ObjectReference objRef, V val);
}
