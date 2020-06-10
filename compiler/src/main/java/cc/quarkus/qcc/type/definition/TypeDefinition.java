package cc.quarkus.qcc.type.definition;

import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

public interface TypeDefinition {
    int getAccess();

    String getName();

    TypeDefinition getSuperclass();

    List<TypeDefinition> getInterfaces();

    List<MethodDefinition<?>> getMethods();

    MethodDefinition<?> findMethod(String name, String desc);
    <V> MethodDefinition<V> findMethod(MethodDescriptor methodDescriptor);

    MethodDefinition<?> resolveMethod(MethodDescriptor methodDescriptor);
    MethodDefinition<?> resolveInterfaceMethod(MethodDescriptor methodDescriptor);
    MethodDefinition<?> resolveInterfaceMethod(MethodDescriptor methodDescriptor, boolean searchingSuper);

    <V> FieldDefinition<V> findField(String name);

    boolean isAssignableFrom(TypeDefinition other);

    <V> V getStatic(FieldDefinition<V> field);

    <V> V getField(FieldDefinition<V> field, ObjectReference objRef);

    <V> void putField(FieldDefinition<V> field, ObjectReference objRef, V val);

    ClassType getType();
}
