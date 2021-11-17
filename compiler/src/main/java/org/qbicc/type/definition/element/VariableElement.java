package org.qbicc.type.definition.element;

import org.qbicc.type.ObjectType;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

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
    private static final VarHandle interpOffsetHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "interpOffset", VarHandle.class, VariableElement.class, int.class);

    private final String name;
    private final TypeDescriptor typeDescriptor;
    private final TypeSignature typeSignature;
    private final TypeAnnotationList visibleTypeAnnotations;
    private final TypeAnnotationList invisibleTypeAnnotations;
    private final TypeParameterContext typeParameterContext;
    private volatile ValueType type;

    // Interpreter caches

    private volatile int interpOffset;

    VariableElement(BuilderImpl builder) {
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
            if (type instanceof ObjectType) {
                type = ((ObjectType)type).getReference();
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

    public int getInterpreterOffset() {
        return interpOffset;
    }

    public void setInterpreterOffset(int value) {
        interpOffset = value;
    }

    public boolean compareAndSetInterpreterOffset(int expect, int update) {
        return interpOffsetHandle.compareAndSet(this, expect, update);
    }

    public interface Builder extends AnnotatedElement.Builder, NamedElement.Builder {
        void setName(final String name);

        void setDescriptor(TypeDescriptor typeDescriptor);

        void setSignature(TypeSignature typeSignature);

        void setVisibleTypeAnnotations(TypeAnnotationList annotations);

        void setInvisibleTypeAnnotations(TypeAnnotationList annotations);

        void setTypeParameterContext(TypeParameterContext typeParameterContext);

        void setType(final ValueType type);

        VariableElement build();

        interface Delegating extends AnnotatedElement.Builder.Delegating, NamedElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setDescriptor(TypeDescriptor typeDescriptor) {
                getDelegate().setDescriptor(typeDescriptor);
            }

            @Override
            default void setSignature(TypeSignature typeSignature) {
                getDelegate().setSignature(typeSignature);
            }

            @Override
            default void setVisibleTypeAnnotations(TypeAnnotationList annotations) {
                getDelegate().setVisibleTypeAnnotations(annotations);
            }

            @Override
            default void setInvisibleTypeAnnotations(TypeAnnotationList annotations) {
                getDelegate().setInvisibleTypeAnnotations(annotations);
            }

            @Override
            default void setTypeParameterContext(TypeParameterContext typeParameterContext) {
                getDelegate().setTypeParameterContext(typeParameterContext);
            }

            @Override
            default void setType(final ValueType type) {
                getDelegate().setType(type);
            }

            @Override
            default void setName(final String name) {
                getDelegate().setName(name);
            }

            @Override
            default VariableElement build() {
                return getDelegate().build();
            }
        }
    }

    static abstract class BuilderImpl extends AnnotatedElement.BuilderImpl implements Builder {
        private String name;
        private TypeDescriptor typeDescriptor;
        private TypeSignature typeSignature;
        private TypeAnnotationList visibleTypeAnnotations = TypeAnnotationList.empty();
        private TypeAnnotationList invisibleTypeAnnotations = TypeAnnotationList.empty();
        private TypeParameterContext typeParameterContext;
        private ValueType type;

        BuilderImpl() {}

        BuilderImpl(final VariableElement original) {
            super(original);
            name = original.name;
            typeDescriptor = original.typeDescriptor;
            typeSignature = original.typeSignature;
            visibleTypeAnnotations = original.visibleTypeAnnotations;
            invisibleTypeAnnotations = original.invisibleTypeAnnotations;
            typeParameterContext = original.typeParameterContext;
            type = original.type;
        }

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
