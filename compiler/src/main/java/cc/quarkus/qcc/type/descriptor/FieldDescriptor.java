package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.type.ObjectReference;

public interface FieldDescriptor<V> {
    TypeDescriptor<V> getTypeDescriptor();
    V get(ObjectReference objRef);
    void put(ObjectReference objRef, V val);
}
