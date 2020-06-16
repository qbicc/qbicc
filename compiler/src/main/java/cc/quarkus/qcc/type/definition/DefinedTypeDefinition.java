package cc.quarkus.qcc.type.definition;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.type.universe.Universe;

/**
 *
 */
public interface DefinedTypeDefinition {
    static DefinedTypeDefinition create(final Universe universe, String name, ByteBuffer buffer) {
        return new DefinedTypeDefinitionImpl(universe, name, buffer);
    }

    Universe getDefiningClassLoader();

    String getName();

    int getAccess();

    String getSuperclassName();

    int getInterfaceCount();

    String getInterfaceName(int index) throws IndexOutOfBoundsException;

    VerifiedTypeDefinition verify() throws VerifyFailedException;

    int getFieldCount();

    String getFieldName(int index) throws IndexOutOfBoundsException;

    int getFieldAccess(int index) throws IndexOutOfBoundsException;

    int getMethodCount();

    String getMethodName(int index) throws IndexOutOfBoundsException;

    int getMethodAccess(int index) throws IndexOutOfBoundsException;
}
