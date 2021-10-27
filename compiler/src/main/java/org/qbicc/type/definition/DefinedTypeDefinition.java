package org.qbicc.type.definition;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.ObjIntConsumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.classfile.BootstrapMethod;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.TypeParameter;
import org.qbicc.type.generic.TypeParameterContext;

/**
 *
 */
public interface DefinedTypeDefinition extends FieldResolver,
                                               MethodResolver,
                                               ConstructorResolver,
                                               InitializerResolver,
                                               TypeParameterContext,
                                               Locatable {

    LoadedTypeDefinition load() throws VerifyFailedException;

    ClassContext getContext();

    @Override
    default Location getLocation() {
        return Location.builder().setClassInternalName(getInternalName()).build();
    }

    String getInternalName();

    boolean internalNameEquals(String internalName);

    boolean internalPackageAndNameEquals(String intPackageName, String className);

    ClassTypeDescriptor getDescriptor();

    ClassSignature getSignature();

    @Override
    default TypeParameter resolveTypeParameter(String parameterName) throws NoSuchElementException {
        TypeParameter parameter = getSignature().getTypeParameter(parameterName);
        if (parameter == null) {
            return getEnclosingTypeParameterContext().resolveTypeParameter(parameterName);
        }
        return parameter;
    }

    @Override
    default TypeParameterContext getEnclosingTypeParameterContext() {
        String enclosingName = getEnclosingClassInternalName();
        if (enclosingName != null && ! isStatic()) {
            DefinedTypeDefinition enclosing = getContext().findDefinedType(enclosingName);
            if (enclosing != null) {
                return enclosing;
            }
        }
        return TypeParameterContext.EMPTY;
    }

    int getModifiers();

    String getEnclosingClassInternalName();

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

    default boolean isFinal() {
        return hasAllModifiersOf(ClassFile.ACC_FINAL);
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

    default FieldElement resolveField(int index, final DefinedTypeDefinition enclosing) {
        return load().getField(index);
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

    default MethodElement resolveMethod(int index, final DefinedTypeDefinition enclosing) {
        return load().getMethod(index);
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
        return load().getConstructor(index);
    }

    // ==================
    // Initializer
    // ==================

    default InitializerElement resolveInitializer(int index, final DefinedTypeDefinition enclosing) {
        return load().getInitializer();
    }

    // ==================
    // Annotations
    // ==================

    List<Annotation> getVisibleAnnotations();

    List<Annotation> getInvisibleAnnotations();

    TypeAnnotationList getVisibleTypeAnnotations();

    TypeAnnotationList getInvisibleTypeAnnotations();

    List<BootstrapMethod> getBootstrapMethods();

    BootstrapMethod getBootstrapMethod(int index);

    interface Builder {
        void setContext(ClassContext context);

        void setInitializer(InitializerResolver resolver, int index);

        void expectFieldCount(int count);

        void addField(FieldResolver resolver, int index);

        void expectMethodCount(int count);

        void addMethod(MethodResolver resolver, int index);

        void expectConstructorCount(int count);

        void addConstructor(ConstructorResolver resolver, int index);

        void setEnclosingClass(String internalName, EnclosingClassResolver resolver, int index);

        void addEnclosedClass(EnclosedClassResolver resolver, int index);

        void setEnclosingMethod(String classInternalName, String methodName, MethodDescriptor methodType);

        void setName(String internalName);

        void setSimpleName(String simpleName);

        void setModifiers(int modifiers);

        void setSuperClassName(String superClassInternalName);

        void expectInterfaceNameCount(int count);

        void addInterfaceName(String interfaceInternalName);

        void setDescriptor(ClassTypeDescriptor descriptor);

        void setSignature(ClassSignature signature);

        void setVisibleAnnotations(List<Annotation> annotations);

        void setInvisibleAnnotations(List<Annotation> annotations);

        void setVisibleTypeAnnotations(TypeAnnotationList annotationList);

        void setInvisibleTypeAnnotations(TypeAnnotationList annotationList);

        void setBootstrapMethods(List<BootstrapMethod> bootstrapMethods);

        void setSuperClass(DefinedTypeDefinition superClass);

        DefinedTypeDefinition build();

        static Builder basic() {
            return new DefinedTypeDefinitionImpl.BuilderImpl();
        }

        interface Delegating extends Builder {
            Builder getDelegate();

            default void setContext(ClassContext context) {
                getDelegate().setContext(context);
            }

            default void setInitializer(InitializerResolver resolver, int index) {
                getDelegate().setInitializer(resolver, index);
            }

            default void expectFieldCount(int count) {
                getDelegate().expectFieldCount(count);
            }

            default void addField(FieldResolver resolver, int index) {
                getDelegate().addField(resolver, index);
            }

            default void expectMethodCount(int count) {
                getDelegate().expectMethodCount(count);
            }

            default void addMethod(MethodResolver resolver, int index) {
                getDelegate().addMethod(resolver, index);
            }

            default void expectConstructorCount(int count) {
                getDelegate().expectConstructorCount(count);
            }

            default void addConstructor(ConstructorResolver resolver, int index) {
                getDelegate().addConstructor(resolver, index);
            }

            default void setEnclosingClass(String internalName, EnclosingClassResolver resolver, int index) {
                getDelegate().setEnclosingClass(internalName, resolver, index);
            }

            default void addEnclosedClass(EnclosedClassResolver resolver, int index) {
                getDelegate().addEnclosedClass(resolver, index);
            }

            default void setEnclosingMethod(String classInternalName, String methodName, MethodDescriptor methodType) {
                getDelegate().setEnclosingMethod(classInternalName, methodName, methodType);
            }

            default void setName(String internalName) {
                getDelegate().setName(internalName);
            }

            default void setSimpleName(String simpleName) {
                getDelegate().setSimpleName(simpleName);
            }

            default void setModifiers(int modifiers) {
                getDelegate().setModifiers(modifiers);
            }

            default void setSuperClassName(String superClassInternalName) {
                getDelegate().setSuperClassName(superClassInternalName);
            }

            default void expectInterfaceNameCount(int count) {
                getDelegate().expectInterfaceNameCount(count);
            }

            default void addInterfaceName(String interfaceInternalName) {
                getDelegate().addInterfaceName(interfaceInternalName);
            }

            default void setDescriptor(ClassTypeDescriptor descriptor) {
                getDelegate().setDescriptor(descriptor);
            }

            default void setSignature(ClassSignature signature) {
                getDelegate().setSignature(signature);
            }

            default void setVisibleAnnotations(List<Annotation> annotations) {
                getDelegate().setVisibleAnnotations(annotations);
            }

            default void setInvisibleAnnotations(List<Annotation> annotations) {
                getDelegate().setInvisibleAnnotations(annotations);
            }

            default void setVisibleTypeAnnotations(TypeAnnotationList annotationList) {
                getDelegate().setVisibleTypeAnnotations(annotationList);
            }

            default void setInvisibleTypeAnnotations(TypeAnnotationList annotationList) {
                getDelegate().setInvisibleTypeAnnotations(annotationList);
            }

            default void setBootstrapMethods(List<BootstrapMethod> bootstrapMethods) {
                getDelegate().setBootstrapMethods(bootstrapMethods);
            }

            default void setSuperClass(DefinedTypeDefinition superClass) {
                getDelegate().setSuperClass(superClass);
            }

            default DefinedTypeDefinition build() {
                return getDelegate().build();
            }
        }
    }
}
