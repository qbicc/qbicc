package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface DefinedFieldDefinition {
    String getName();

    int getModifiers();

    DefinedTypeDefinition getEnclosingTypeDefinition();

    ResolvedFieldDefinition resolve() throws ResolutionFailedException;


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

    default boolean isVolatile() {
        return hasAllModifiersOf(ClassFile.ACC_VOLATILE);
    }

    interface Builder {
        void setModifiers(int modifiers);

        DefinedFieldDefinition build();
    }
}
