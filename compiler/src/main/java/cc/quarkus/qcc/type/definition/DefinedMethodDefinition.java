package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;

/**
 *
 */
public interface DefinedMethodDefinition {
    String getName();

    int getModifiers();

    int getParameterCount();

    String getParameterName(int index) throws IndexOutOfBoundsException;

    Type getParameterType(int index) throws IndexOutOfBoundsException;

    Type getReturnType();

    VerifiedTypeDefinition getEnclosingType();

    VerifiedMethodDefinition verify() throws VerifyFailedException;

}
