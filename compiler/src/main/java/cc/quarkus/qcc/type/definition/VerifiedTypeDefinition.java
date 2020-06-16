package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 *
 */
public interface VerifiedTypeDefinition extends DefinedTypeDefinition {
    default VerifiedTypeDefinition verify() {
        return this;
    }

    ClassType getClassType();

    boolean isAssignableFrom(VerifiedTypeDefinition other);

    VerifiedTypeDefinition getSuperclass();

    VerifiedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    PreparedTypeDefinition prepare() throws PrepareFailedException;

    FieldDefinition resolveField(FieldDescriptor fieldDescriptor);

    FieldDefinition findField(String name);

    FieldDefinition getField(int index) throws IndexOutOfBoundsException;

    VerifiedMethodDefinition findMethod(String name, String desc);

    VerifiedMethodDefinition findMethod(MethodDescriptor methodDescriptor);

    VerifiedMethodDefinition resolveMethod(MethodDescriptor methodDescriptor);

    VerifiedMethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor);

    VerifiedMethodDefinition resolveInterfaceMethod(MethodDescriptor methodDescriptor, boolean searchingSuper);

}
