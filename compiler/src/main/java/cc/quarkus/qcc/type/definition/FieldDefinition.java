package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;

public interface FieldDefinition<V> extends FieldDescriptor {
    TypeDefinition getTypeDefinition();
    String getName();
    V get(ObjectReference objRef);
    void put(ObjectReference objRef, V val);
}
