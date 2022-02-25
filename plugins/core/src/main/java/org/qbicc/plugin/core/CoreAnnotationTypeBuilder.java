package org.qbicc.plugin.core;

import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.annotation.EnumConstantAnnotationValue;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 * A type builder which applies the core annotations.
 */
public final class CoreAnnotationTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final DefinedTypeDefinition.Builder delegate;

    private final ClassTypeDescriptor autoQueued;
    private final ClassTypeDescriptor noSideEffects;
    private final ClassTypeDescriptor hidden;
    private final ClassTypeDescriptor jdkHidden;
    private final ClassTypeDescriptor noReflect;
    private final ClassTypeDescriptor noReturn;
    private final ClassTypeDescriptor noThrow;
    private final ClassTypeDescriptor inline;
    private final ClassTypeDescriptor fold;

    public CoreAnnotationTypeBuilder(final ClassContext classCtxt, DefinedTypeDefinition.Builder delegate) {
        this.delegate = delegate;

        autoQueued = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/AutoQueued");
        noSideEffects = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoSideEffects");
        hidden = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/Hidden");
        jdkHidden = ClassTypeDescriptor.synthesize(classCtxt, "jdk/internal/vm/annotation/Hidden");
        noReflect = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoReflect");
        noReturn = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoReturn");
        noThrow = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoThrow");
        inline = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/Inline");
        fold = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/Fold");
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void addMethod(MethodResolver resolver, int index, String name, MethodDescriptor descriptor) {
        Delegating.super.addMethod(new MethodResolver() {
            @Override
            public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
                MethodElement methodElement = resolver.resolveMethod(index, enclosing, builder);
                for (Annotation annotation : methodElement.getInvisibleAnnotations()) {
                    if (annotation.getDescriptor().equals(autoQueued)) {
                        enclosing.getContext().getCompilationContext().registerAutoQueuedElement(methodElement);
                    } else if (annotation.getDescriptor().equals(noSideEffects)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_SIDE_EFFECTS);
                    } else if (annotation.getDescriptor().equals(hidden)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_HIDDEN);
                    } else if (annotation.getDescriptor().equals(noReturn)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_RETURN);
                    } else if (annotation.getDescriptor().equals(noReflect)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_REFLECT);
                    } else if (annotation.getDescriptor().equals(noThrow)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_THROW);
                    } else if (annotation.getDescriptor().equals(inline)) {
                        AnnotationValue value = annotation.getValue("value");
                        if (value instanceof EnumConstantAnnotationValue ec) {
                            if (ec.getValueName().equals("ALWAYS")) {
                                methodElement.setModifierFlags(ClassFile.I_ACC_ALWAYS_INLINE);
                            } else if (ec.getValueName().equals("NEVER")) {
                                methodElement.setModifierFlags(ClassFile.I_ACC_NEVER_INLINE);
                            }
                        }
                    } else if (annotation.getDescriptor().equals(fold)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_FOLD);
                    }
                }
                for (Annotation annotation : methodElement.getVisibleAnnotations()) {
                    if (annotation.getDescriptor().equals(jdkHidden)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_HIDDEN);
                    }
                }
                return methodElement;
            }
        }, index, name, descriptor);
    }

    @Override
    public void addField(FieldResolver resolver, int index, String name, TypeDescriptor descriptor) {
        Delegating.super.addField(new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                FieldElement fieldElement = resolver.resolveField(index, enclosing, builder);
                for (Annotation annotation : fieldElement.getInvisibleAnnotations()) {
                    if (annotation.getDescriptor().equals(noReflect)) {
                        fieldElement.setModifierFlags(ClassFile.I_ACC_NO_REFLECT);
                    }
                }
                return fieldElement;
            }
        }, index, name, descriptor);
    }

    @Override
    public void addConstructor(ConstructorResolver resolver, int index, MethodDescriptor descriptor) {
        Delegating.super.addConstructor(new ConstructorResolver() {
            @Override
            public ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing, ConstructorElement.Builder builder) {
                ConstructorElement constructorElement = resolver.resolveConstructor(index, enclosing, builder);
                for (Annotation annotation : constructorElement.getInvisibleAnnotations()) {
                    if (annotation.getDescriptor().equals(autoQueued)) {
                        enclosing.getContext().getCompilationContext().registerAutoQueuedElement(constructorElement);
                    } else if (annotation.getDescriptor().equals(noSideEffects)) {
                        constructorElement.setModifierFlags(ClassFile.I_ACC_NO_SIDE_EFFECTS);
                    } else if (annotation.getDescriptor().equals(hidden)) {
                        constructorElement.setModifierFlags(ClassFile.I_ACC_HIDDEN);
                    } else if (annotation.getDescriptor().equals(noReturn)) {
                        constructorElement.setModifierFlags(ClassFile.I_ACC_NO_RETURN);
                    } else if (annotation.getDescriptor().equals(noReflect)) {
                        constructorElement.setModifierFlags(ClassFile.I_ACC_NO_REFLECT);
                    } else if (annotation.getDescriptor().equals(noThrow)) {
                        constructorElement.setModifierFlags(ClassFile.I_ACC_NO_THROW);
                    } else if (annotation.getDescriptor().equals(inline)) {
                        AnnotationValue value = annotation.getValue("value");
                        if (value instanceof EnumConstantAnnotationValue ec) {
                            if (ec.getValueName().equals("ALWAYS")) {
                                constructorElement.setModifierFlags(ClassFile.I_ACC_ALWAYS_INLINE);
                            } else if (ec.getValueName().equals("NEVER")) {
                                constructorElement.setModifierFlags(ClassFile.I_ACC_NEVER_INLINE);
                            }
                        }
                    }
                }
                for (Annotation annotation : constructorElement.getVisibleAnnotations()) {
                    if (annotation.getDescriptor().equals(jdkHidden)) {
                        constructorElement.setModifierFlags(ClassFile.I_ACC_HIDDEN);
                    }
                }
                return constructorElement;
            }
        }, index, descriptor);
    }
}
