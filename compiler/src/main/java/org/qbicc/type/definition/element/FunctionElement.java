package org.qbicc.type.definition.element;

import java.util.List;

import org.qbicc.type.FunctionType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.util.ResolutionUtil;

/**
 * An element that represents some function.
 */
public final class FunctionElement extends InvokableElement implements NamedElement {
    private final String name;

    FunctionElement(final BuilderImpl builder) {
        super(builder);
        Assert.checkNotNullParam("builder.type", builder.type);
        name = Assert.checkNotNullParam("builder.name", builder.name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

    @Override
    FunctionType computeType() {
        return ResolutionUtil.resolveFunctionType(getEnclosingType().getContext(), this, getDescriptor(), getSignature());
    }

    @Override
    public <T, R> R accept(ElementVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public String toString() {
        return "function " + getName();
    }

    public static Builder builder(String name, MethodDescriptor descriptor, int index) {
        return new BuilderImpl(name, descriptor, index);
    }

    public interface Builder extends InvokableElement.Builder, NamedElement.Builder {

        FunctionElement build();

        void setType(FunctionType type);

        void addVisibleAnnotations(List<Annotation> annotations);

        void addInvisibleAnnotations(List<Annotation> annotations);

        void setReturnVisibleTypeAnnotations(TypeAnnotationList returnVisibleTypeAnnotations);

        void setReturnInvisibleTypeAnnotations(TypeAnnotationList returnInvisibleTypeAnnotations);

        interface Delegating extends InvokableElement.Builder.Delegating, NamedElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setType(FunctionType type) {
                getDelegate().setType(type);
            }

            @Override
            default FunctionElement build() {
                return getDelegate().build();
            }

            @Override
            default void addVisibleAnnotations(List<Annotation> annotations) {
                getDelegate().addVisibleAnnotations(annotations);
            }

            @Override
            default void addInvisibleAnnotations(List<Annotation> annotations) {
                 getDelegate().addInvisibleAnnotations(annotations);
            }

            @Override
            default void setReturnVisibleTypeAnnotations(final TypeAnnotationList annotations) {
                getDelegate().setReturnVisibleTypeAnnotations(annotations);
            }

            @Override
            default void setReturnInvisibleTypeAnnotations(final TypeAnnotationList annotations) {
                getDelegate().setReturnInvisibleTypeAnnotations(annotations);
            }
        }
    }

    static final class BuilderImpl extends InvokableElement.BuilderImpl implements Builder {
        private final String name;

        BuilderImpl(String name, MethodDescriptor descriptor, int index) {
            super(descriptor, index);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public FunctionElement build() {
            return new FunctionElement(this);
        }

        @Override
        public void setType(FunctionType type) {
            super.setType(Assert.checkNotNullParam("type", type));
        }

        @Override
        public void addVisibleAnnotations(List<Annotation> annotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void addInvisibleAnnotations(List<Annotation> annotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setReturnVisibleTypeAnnotations(TypeAnnotationList returnVisibleTypeAnnotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setReturnInvisibleTypeAnnotations(TypeAnnotationList returnInvisibleTypeAnnotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

    }
}
