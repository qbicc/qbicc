package cc.quarkus.qcc.interpret;

import java.util.Collection;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.TypeDefinition;

public interface Heap {
    ObjectReference newObject(TypeDefinition type);
    Collection<ObjectReference> allocated();
}
