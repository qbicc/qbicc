package org.qbicc.plugin.core;

import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 * A type builder which applies the core annotations.
 */
public final class CoreAnnotationTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final DefinedTypeDefinition.Builder delegate;

    private final ClassTypeDescriptor noSideEffects;
    private final ClassTypeDescriptor hidden;
    private final ClassTypeDescriptor noReturn;
    private final ClassTypeDescriptor noThrow;

    public CoreAnnotationTypeBuilder(final ClassContext classCtxt, DefinedTypeDefinition.Builder delegate) {
        this.delegate = delegate;

        noSideEffects = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoSideEffects");
        hidden = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/Hidden");
        noReturn = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoReturn");
        noThrow = ClassTypeDescriptor.synthesize(classCtxt, "org/qbicc/runtime/NoThrow");
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void addMethod(MethodResolver resolver, int index) {
        Delegating.super.addMethod(new MethodResolver() {
            @Override
            public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing) {
                MethodElement methodElement = resolver.resolveMethod(index, enclosing);
                for (Annotation annotation : methodElement.getInvisibleAnnotations()) {
                    if (annotation.getDescriptor().equals(noSideEffects)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_SIDE_EFFECTS);
                    } else if (annotation.getDescriptor().equals(hidden)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_HIDDEN);
                    } else if (annotation.getDescriptor().equals(noReturn)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_RETURN);
                    } else if (annotation.getDescriptor().equals(noThrow)) {
                        methodElement.setModifierFlags(ClassFile.I_ACC_NO_THROW);
                    }
                }
                return methodElement;
            }
        }, index);
    }
}
