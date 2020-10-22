package cc.quarkus.qcc.type.definition;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 * A module definition.
 */
public interface ModuleDefinition {
    static ModuleDefinition create(ByteBuffer buffer) {
        return new ModuleDefinitionImpl(buffer);
    }

    String getName();

    String getVersion();

    int getModifiers();

    default boolean hasAllModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    default boolean hasNoModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    default boolean isOpen() {
        return hasAllModifiersOf(ClassFile.ACC_OPEN);
    }

    default boolean isSynthetic() {
        return hasAllModifiersOf(ClassFile.ACC_SYNTHETIC);
    }

    default boolean isMandated() {
        return hasAllModifiersOf(ClassFile.ACC_MANDATED);
    }

    int getPackageCount();

    String getPackage(int index) throws IndexOutOfBoundsException;

    int getRequiresCount();

    String getRequiresName(int index) throws IndexOutOfBoundsException;

    int getRequiresModifiers(int index) throws IndexOutOfBoundsException;

    String getRequiresVersion(int index) throws IndexOutOfBoundsException;

    int getExportsCount();

    String getExportsPackageName(int index) throws IndexOutOfBoundsException;

    int getExportsModifiers(int index) throws IndexOutOfBoundsException;

    int getExportsToCount(int index) throws IndexOutOfBoundsException;

    String getExportsTo(int exportsIndex, int exportsToIndex) throws IndexOutOfBoundsException;

    int getOpensCount();

    String getOpensPackageName(int index) throws IndexOutOfBoundsException;

    int getOpensModifiers(int index) throws IndexOutOfBoundsException;

    int getOpensToCount(int index) throws IndexOutOfBoundsException;

    String getOpensTo(int opensIndex, int opensToIndex) throws IndexOutOfBoundsException;

    int getUsesCount();

    String getUses(int index) throws IndexOutOfBoundsException;

    int getProvidesCount();

    String getProvides(int index) throws IndexOutOfBoundsException;

    int getProvidesWithCount(int index);

    String getProvidesWith(int providesIndex, int providesWithIndex) throws IndexOutOfBoundsException;
}
