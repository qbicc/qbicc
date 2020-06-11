package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.universe.Universe;

public interface TypeDefinition {
    Universe getUniverse();

    int getAccess();

    String getName();

    TypeDefinition getSuperclass();

    List<TypeDefinition> getInterfaces();

    List<MethodDefinition> getMethods();

    MethodDefinition findMethod(String name, String desc);
    MethodDefinition findMethod(MethodDescriptor methodDescriptor);

    MethodDefinition resolveMethod(MethodDescriptor methodDescriptor);
    MethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor);
    MethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor, boolean searchingSuper);

    List<FieldDefinition> getFields();

    FieldDefinition resolveField(FieldDescriptor fieldDescriptor);

    FieldDefinition findField(String name);

    boolean isAssignableFrom(TypeDefinition other);

    Object getStatic(FieldDefinition field);

    Object getField(FieldDefinition field, ObjectReference objRef);

    void putField(FieldDefinition field, ObjectReference objRef, Object val);

    ClassType getType();
}
