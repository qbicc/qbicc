package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 *
 */
public interface ResolvedTypeDefinition extends VerifiedTypeDefinition {
    default ResolvedTypeDefinition verify() {
        return this;
    }

    default ResolvedTypeDefinition resolve() {
        return this;
    }

    ResolvedTypeDefinition getSuperClass();

    ResolvedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    PreparedTypeDefinition prepare() throws PrepareFailedException;

    ResolvedFieldDefinition getFieldDefinition(int index) throws IndexOutOfBoundsException;

    ResolvedFieldDefinition resolveField(Type type, String name);

    ResolvedFieldDefinition findField(String name);

    ResolvedMethodDefinition getMethodDefinition(int index) throws IndexOutOfBoundsException;

    ResolvedMethodDefinition resolveMethod(MethodIdentifier identifier);

    ResolvedMethodDefinition resolveInterfaceMethod(MethodIdentifier identifier);

    ResolvedMethodDefinition resolveInterfaceMethod(MethodIdentifier identifier, boolean searchingSuper);

}
