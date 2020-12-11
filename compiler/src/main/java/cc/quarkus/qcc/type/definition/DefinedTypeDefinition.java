package cc.quarkus.qcc.type.definition;

import java.util.List;
import java.util.function.ObjIntConsumer;

import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.generic.ClassSignature;

/**
 *
 */
public interface DefinedTypeDefinition extends FieldResolver,
                                               MethodResolver,
                                               ConstructorResolver,
                                               InitializerResolver {

    ValidatedTypeDefinition validate() throws VerifyFailedException;

    ClassContext getContext();

    String getInternalName();

    boolean internalNameEquals(String internalName);

    boolean internalPackageAndNameEquals(String intPackageName, String className);

    ClassSignature getSignature();

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

    default FieldElement resolveField(int index, final DefinedTypeDefinition enclosing) {
        return validate().getField(index);
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
        return validate().getMethod(index);
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

    // ==================
    // Initializer
    // ==================

    default InitializerElement resolveInitializer(int index, final DefinedTypeDefinition enclosing) {
        return validate().getInitializer();
    }

    // ==================
    // Annotations
    // ==================

    List<Annotation> getVisibleAnnotations();

    List<Annotation> getInvisibleAnnotations();

    TypeAnnotationList getVisibleTypeAnnotations();

    TypeAnnotationList getInvisibleTypeAnnotations();

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

        void setSignature(ClassSignature signature);

        void setVisibleAnnotations(List<Annotation> annotations);

        void setInvisibleAnnotations(List<Annotation> annotations);

        void setVisibleTypeAnnotations(TypeAnnotationList annotationList);

        void setInvisibleTypeAnnotations(TypeAnnotationList annotationList);

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

            default void setName(String internalName) {
                getDelegate().setName(internalName);
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

            default DefinedTypeDefinition build() {
                return getDelegate().build();
            }
        }
    }
}
