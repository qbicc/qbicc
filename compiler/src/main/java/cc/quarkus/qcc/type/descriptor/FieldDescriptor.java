package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QType;

public interface FieldDescriptor<V extends QType> {
    TypeDescriptor<V> getTypeDescriptor();
    V get(ObjectReference objRef);
    void put(ObjectReference objRef, V val);
}
