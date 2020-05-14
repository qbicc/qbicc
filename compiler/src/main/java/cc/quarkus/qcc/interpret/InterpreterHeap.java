package cc.quarkus.qcc.interpret;

import java.util.Collection;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public interface InterpreterHeap {
    ObjectReference newObject(TypeDefinition type);
    Collection<ObjectReference> allocated();
}
