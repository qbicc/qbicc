package cc.quarkus.qcc.type;

public interface FieldDescriptor<V> {
    TypeDescriptor<V> getTypeDescriptor();
    V get(ObjectReference objRef);
    void put(ObjectReference objRef, V val);
}
