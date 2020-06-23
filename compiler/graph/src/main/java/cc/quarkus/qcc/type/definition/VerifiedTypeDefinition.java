package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;

/**
 *
 */
public interface VerifiedTypeDefinition extends DefinedTypeDefinition {
    default VerifiedTypeDefinition verify() {
        return this;
    }

    ClassType getClassType();

    VerifiedTypeDefinition getSuperClass();

    VerifiedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    ResolvedTypeDefinition resolve() throws ResolutionFailedException;
}
