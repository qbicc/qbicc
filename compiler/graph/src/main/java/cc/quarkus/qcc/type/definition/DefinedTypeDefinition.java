package cc.quarkus.qcc.type.definition;

import java.util.function.ObjIntConsumer;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterElement;
import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface DefinedTypeDefinition extends FieldElement.TypeResolver, FieldResolver,
                                               MethodElement.TypeResolver, MethodResolver,
                                               ConstructorElement.TypeResolver, ConstructorResolver,
                                               ParameterElement.TypeResolver,
                                               InitializerResolver {

    ValidatedTypeDefinition validate() throws VerifyFailedException;

    ClassContext getContext();

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

    default ValueType resolveFieldType(long argument) throws ResolutionFailedException {
        return resolveField((int) argument, this).getType();
    }

    default FieldElement resolveField(int index, final DefinedTypeDefinition enclosing) {
        return validate().getField(index);
    }

    default boolean hasClass2FieldType(long argument) {
        return resolveField((int) argument, this).hasClass2Type();
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

    default ValueType resolveParameterType(int methodIdx, int paramIdx) throws ResolutionFailedException {
        return resolveMethod(methodIdx, this).getParameter(paramIdx).getType();
    }

    default boolean hasClass2ParameterType(int methodArg, int paramArg) {
        return resolveMethod(methodArg, this).getParameter(paramArg).hasClass2Type();
    }

    default MethodElement resolveMethod(int index, final DefinedTypeDefinition enclosing) {
        return validate().getMethod(index);
    }

    default MethodDescriptor resolveMethodDescriptor(int argument) throws ResolutionFailedException {
        return resolveMethod(argument, this).getDescriptor();
    }

    default boolean hasClass2ReturnType(int argument) {
        return resolveMethod(argument, this).hasClass2ReturnType();
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

    default ConstructorElement resolveConstructor(int index, final DefinedTypeDefinition enclosing) {
        return validate().getConstructor(index);
    }

    default ConstructorDescriptor resolveConstructorDescriptor(int argument) throws ResolutionFailedException {
        return resolveConstructor(argument, this).getDescriptor();
    }

    // ==================
    // Initializer
    // ==================

    default InitializerElement resolveInitializer(int index, final DefinedTypeDefinition enclosing) {
        return validate().getInitializer();
    }

    // ==================
    // Annotations
    // ==================

    int getVisibleAnnotationCount();

    Annotation getVisibleAnnotation(int index);

    int getInvisibleAnnotationCount();

    Annotation getInvisibleAnnotation(int index);

    interface Builder {
        void setContext(ClassContext context);

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
