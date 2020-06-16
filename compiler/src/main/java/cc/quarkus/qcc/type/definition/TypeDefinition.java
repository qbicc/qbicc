package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.universe.Universe;

public interface TypeDefinition {
    Universe getUniverse();

    int getAccess();

    String getName();

    TypeDefinition getSuperclass();

    List<TypeDefinition> getInterfaces();

    List<MethodDefinition> getMethods();

    MethodDefinition findMethod(String name, String desc);
    MethodDefinition findMethod(MethodIdentifier methodDescriptor);

    MethodDefinition resolveMethod(MethodIdentifier methodDescriptor);
    MethodDefinition resolveInterfaceMethod(MethodIdentifier methodDescriptor);
    MethodDefinition resolveInterfaceMethod(MethodIdentifier methodDescriptor, boolean searchingSuper);

    List<ResolvedFieldDefinition> getFields();

    ResolvedFieldDefinition resolveField(FieldDescriptor fieldDescriptor);

    ResolvedFieldDefinition findField(String name);

    boolean isAssignableFrom(TypeDefinition other);

    Object getStatic(ResolvedFieldDefinition field);

    Object getField(ResolvedFieldDefinition field, ObjectReference objRef);

    void putField(ResolvedFieldDefinition field, ObjectReference objRef, Object val);

    ClassType getType();
}
