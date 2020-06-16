package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface DefinedMethodDefinition {
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

    default boolean isNative() {
        return hasAllModifiersOf(ClassFile.ACC_NATIVE);
    }

    default boolean isVarArgs() {
        return hasAllModifiersOf(ClassFile.ACC_VARARGS);
    }

    int getParameterCount();

    String getParameterName(int index) throws IndexOutOfBoundsException;

    DefinedTypeDefinition getEnclosingTypeDefinition();

    ResolvedMethodDefinition resolve() throws ResolutionFailedException;

    boolean hasMethodBody();

    DefinedMethodBody getMethodBody();
}
