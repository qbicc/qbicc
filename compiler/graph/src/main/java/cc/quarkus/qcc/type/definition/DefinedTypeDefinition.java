package cc.quarkus.qcc.type.definition;

import java.util.function.ObjIntConsumer;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.JavaClass;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface DefinedTypeDefinition extends FieldElement.TypeResolver, FieldResolver,
                                               MethodElement.TypeResolver, MethodResolver,
                                               ConstructorResolver,
                                               ParameterElement.TypeResolver,
                                               InitializerResolver {

    VerifiedTypeDefinition verify() throws VerifyFailedException;

    /**
     * Get the defining class loader object for this type.  The {@code null} value is used to indicate
     * the bootstrap class loader.
     *
     * @return the defining class loader, or {@code null} for the bootstrap class loader
     */
    JavaObject getDefiningClassLoader();

    JavaClass getJavaClass();

    String getInternalName();

    boolean internalNameEquals(String internalName);

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

    // ==================
    // Superclass
    // ==================

    boolean hasSuperClass();

    String getSuperClassInternalName();

    boolean superClassInternalNameEquals(String internalName);

    // ==================
    // Interfaces
    // ==================

    int getInterfaceCount();

    default void eachInterfaceIndex(ObjIntConsumer<DefinedTypeDefinition> consumer) {
        int count = getInterfaceCount();
        for (int i = 0; i < count; i ++) {
            consumer.accept(this, i);
        }
    }

    String getInterfaceInternalName(int index) throws IndexOutOfBoundsException;

    boolean interfaceInternalNameEquals(int index, String internalName) throws IndexOutOfBoundsException;

    // ==================
    // Fields
    // ==================

    int getFieldCount();

    default void eachFieldIndex(ObjIntConsumer<DefinedTypeDefinition> consumer) {
        int count = getFieldCount();
        for (int i = 0; i < count; i ++) {
            consumer.accept(this, i);
        }
    }

    default Type resolveFieldType(long argument) throws ResolutionFailedException {
        return resolveField((int) argument).getType();
    }

    default FieldElement resolveField(int index) {
        return verify().getField(index);
    }

    // ==================
    // Methods
    // ==================

    int getMethodCount();

    default void eachMethodIndex(ObjIntConsumer<DefinedTypeDefinition> consumer) {
        int count = getMethodCount();
        for (int i = 0; i < count; i ++) {
            consumer.accept(this, i);
        }
    }

    default Type resolveMethodReturnType(long argument) throws ResolutionFailedException {
        return verify().getField((int) argument).getType();
    }

    default Type resolveParameterType(long argument) throws ResolutionFailedException {
        return resolveMethod((int) (argument << 8)).getParameter((int) (argument & 0xff)).getType();
    }

    default MethodElement resolveMethod(int index) {
        return verify().getMethod(index);
    }

    default long encodeParameterIndex(int index, int paramIndex) {
        Assert.checkMinimumParameter("index", 0, index);
        Assert.checkMinimumParameter("paramIndex", 0, paramIndex);
        Assert.checkMaximumParameter("paramIndex", 255, paramIndex);
        return ((long)index << 8) | paramIndex;
    }

    // ==================
    // Constructors
    // ==================

    int getConstructorCount();

    default void eachConstructorIndex(ObjIntConsumer<DefinedTypeDefinition> consumer) {
        int count = getConstructorCount();
        for (int i = 0; i < count; i ++) {
            consumer.accept(this, i);
        }
    }

    default ConstructorElement resolveConstructor(int index) {
        return verify().getConstructor(index);
    }

    // ==================
    // Initializer
    // ==================

    default InitializerElement resolveInitializer(int index) {
        return verify().getInitializer();
    }

    // ==================
    // Annotations
    // ==================

    int getVisibleAnnotationCount();

    Annotation getVisibleAnnotation(int index);

    int getInvisibleAnnotationCount();

    Annotation getInvisibleAnnotation(int index);

    interface Builder {
        void setJavaClass(final JavaClass javaClass);

        void setDefiningClassLoader(JavaObject classLoader);

        void setInitializer(InitializerResolver resolver, int index);

        void expectFieldCount(int count);

        void addField(FieldResolver resolver, int index);

        void expectMethodCount(int count);

        void addMethod(MethodResolver resolver, int index);

        void expectConstructorCount(int count);

        void addConstructor(ConstructorResolver resolver, int index);

        void setName(String internalName);

        void setModifiers(int modifiers);

        void setSuperClassName(String superClassInternalName);

        void expectInterfaceNameCount(int count);

        void addInterfaceName(String interfaceInternalName);

        void expectVisibleAnnotationCount(int count);

        void addVisibleAnnotation(Annotation annotation);

        void expectInvisibleAnnotationCount(int count);

        void addInvisibleAnnotation(Annotation annotation);

        DefinedTypeDefinition build();

        static Builder basic() {
            return new DefinedTypeDefinitionImpl.BuilderImpl();
        }

        interface Factory {
            Builder construct(Builder delegate);
        }
    }
}
