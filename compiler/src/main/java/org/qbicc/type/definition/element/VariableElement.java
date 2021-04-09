package org.qbicc.type.definition.element;

import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import io.smallrye.common.constraint.Assert;

/**
 * An element representing a variable of some kind.
 */
public abstract class VariableElement extends AnnotatedElement implements NamedElement {
    private final String name;
    private final TypeDescriptor typeDescriptor;
    private final TypeSignature typeSignature;
    private final TypeAnnotationList visibleTypeAnnotations;
    private final TypeAnnotationList invisibleTypeAnnotations;
    private final TypeParameterContext typeParameterContext;
    private volatile ValueType type;

    VariableElement(Builder builder) {
        super(builder);
        this.name = builder.name;
        this.typeDescriptor = Assert.checkNotNullParam("builder.typeDescriptor", builder.typeDescriptor);
        this.typeSignature = Assert.checkNotNullParam("builder.typeSignature", builder.typeSignature);
        this.visibleTypeAnnotations = builder.visibleTypeAnnotations;
        this.invisibleTypeAnnotations = builder.invisibleTypeAnnotations;
        this.typeParameterContext = Assert.checkNotNullParam("builder.typeParameterContext", builder.typeParameterContext);
        ValueType type = builder.type;
        if (type != null) {
            this.type = type;
        }
    }

    public String getName() {
        return name;
    }

    public TypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }

    public TypeSignature getTypeSignature() {
        return typeSignature;
    }

    public TypeAnnotationList getVisibleTypeAnnotations() {
        return visibleTypeAnnotations;
    }

    public TypeAnnotationList getInvisibleTypeAnnotations() {
        return invisibleTypeAnnotations;
    }

    /**
     * Get or resolve the type of the variable.  This may cause classes to be loaded, resolved, and/or initialized
     * recursively.
     *
     * @return the resolved type of the variable
     */
    public ValueType getType() {
        ClassContext classContext = getEnclosingType().getContext();
        ValueType type = this.type;
        if (type == null) {
            type = resolveTypeDescriptor(classContext, typeParameterContext);
            if (type instanceof ReferenceType) {
                // all fields are considered nullable for now
                // todo: add a flag for non-nullable fields
                // todo: initial heap final fields are non-nullable
                type = ((ReferenceType) type).asNullable();
            }
            this.type = type;
        }
        return type;
    }

    ValueType resolveTypeDescriptor(ClassContext classContext, TypeParameterContext paramCtxt) {
        return classContext.resolveTypeFromDescriptor(
                        getTypeDescriptor(),
                        paramCtxt,
                        getTypeSignature(),
                        getVisibleTypeAnnotations(),
                        getInvisibleTypeAnnotations());
    }

    public boolean isFinal() {
        return hasAllModifiersOf(ClassFile.ACC_FINAL);
    }

    public boolean hasClass2Type() {
        return getTypeDescriptor().isClass2();
    }

    public static abstract class Builder extends AnnotatedElement.Builder implements NamedElement.Builder {
        private String name;
        private TypeDescriptor typeDescriptor;
        private TypeSignature typeSignature;
        private TypeAnnotationList visibleTypeAnnotations = TypeAnnotationList.empty();
        private TypeAnnotationList invisibleTypeAnnotations = TypeAnnotationList.empty();
        private TypeParameterContext typeParameterContext;
        private ValueType type;

        Builder() {}

        public void setName(final String name) {
            this.name = name;
        }

        public void setDescriptor(TypeDescriptor typeDescriptor) {
            this.typeDescriptor = Assert.checkNotNullParam("typeDescriptor", typeDescriptor);
        }

        public void setSignature(TypeSignature typeSignature) {
            this.typeSignature = Assert.checkNotNullParam("typeSignature", typeSignature);
        }

        public void setVisibleTypeAnnotations(TypeAnnotationList annotations) {
            this.visibleTypeAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public void setInvisibleTypeAnnotations(TypeAnnotationList annotations) {
            this.invisibleTypeAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public void setTypeParameterContext(TypeParameterContext typeParameterContext) {
            this.typeParameterContext = typeParameterContext;
        }

        public void setType(final ValueType type) {
            this.type = type;
        }

        public abstract VariableElement build();
    }
}
