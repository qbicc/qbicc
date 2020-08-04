package cc.quarkus.qcc.type.definition;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import cc.quarkus.qcc.type.universe.Universe;

/**
 *
 */
public interface DefinedTypeDefinition {
    static DefinedTypeDefinition create(final Universe universe, String name, ByteBuffer buffer) {
        return new DefinedTypeDefinitionImpl(universe, name, buffer);
    }

    default boolean isArray() {
        return false;
    }

    Universe getDefiningClassLoader();

    String getName();

    int getModifiers();

    default boolean hasAllModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    default boolean hasNoModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    default boolean isStatic() {
        return hasAllModifiersOf(ClassFile.ACC_STATIC);
    }

    default boolean isPublic() {
        return hasAllModifiersOf(ClassFile.ACC_PUBLIC);
    }

    default boolean isProtected() {
        return hasAllModifiersOf(ClassFile.ACC_PROTECTED);
    }

    default boolean isPackagePrivate() {
        return hasNoModifiersOf(ClassFile.ACC_PUBLIC | ClassFile.ACC_PROTECTED | ClassFile.ACC_PRIVATE);
    }

    default boolean isPrivate() {
        return hasAllModifiersOf(ClassFile.ACC_PRIVATE);
    }

    default boolean isAbstract() {
        return hasAllModifiersOf(ClassFile.ACC_ABSTRACT);
    }

    default boolean isInterface() {
        return hasAllModifiersOf(ClassFile.ACC_INTERFACE);
    }

    String getSuperClassName();

    int getInterfaceCount();

    String getInterfaceName(int index) throws IndexOutOfBoundsException;

    VerifiedTypeDefinition verify() throws VerifyFailedException;

    int getFieldCount();

    DefinedFieldDefinition getFieldDefinition(int index) throws IndexOutOfBoundsException;

    default void eachField(Consumer<DefinedFieldDefinition> consumer) {
        int count = getFieldCount();
        for (int i = 0; i < count; i++) {
            consumer.accept(getFieldDefinition(i));
        }
    }

    int getMethodCount();

    DefinedMethodDefinition getMethodDefinition(int index) throws IndexOutOfBoundsException;

    interface Builder {
        void setSuperClassName(String name);

        void addInterfaceName(String name);

        void setDefiningClassLoader(Universe universe);

        // XXX XXX XXX
        DefinedFieldDefinition.Builder addField(String name);

        DefinedMethodDefinition.Builder addMethod(String name);

        DefinedTypeDefinition build();
    }
}
