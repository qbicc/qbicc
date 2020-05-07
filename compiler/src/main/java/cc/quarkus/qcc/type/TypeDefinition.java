package cc.quarkus.qcc.type;

import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.interpret.Heap;
import cc.quarkus.qcc.interpret.SimpleHeap;

public interface TypeDefinition {
    int getAccess();

    String getName();

    TypeDefinition getSuperclass();

    List<TypeDefinition> getInterfaces();

    Set<MethodDefinition> getMethods();

    MethodDefinition findMethod(String name, String desc);

    MethodDefinition findMethod(MethodDescriptor methodDescriptor);

    <V> FieldDefinition<V> findField(String name);

    boolean isAssignableFrom(TypeDefinition other);

    TypeDescriptor<ObjectReference> getTypeDescriptor();

    <V> V getStatic(FieldDefinition<V> field);

    <V> V getField(FieldDefinition<V> field, ObjectReference objRef);

    <V> void putField(FieldDefinition<V> field, ObjectReference objRef, V val);

    default ObjectReference newInstance(Object... arguments) {
        return newInstance(new SimpleHeap(), arguments);
    }

    default ObjectReference newInstance(List<Object> arguments) {
        return newInstance(new SimpleHeap(), arguments);
    }

    ObjectReference newInstance(Heap heap, Object... arguments);

    ObjectReference newInstance(Heap heap, List<Object> arguments);
}
