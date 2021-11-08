package org.qbicc.type.definition.element;

import java.util.List;

import org.qbicc.type.FunctionType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import io.smallrye.common.constraint.Assert;

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
    public <T, R> R accept(ElementVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public String toString() {
        return "function " + getName();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends InvokableElement.Builder, NamedElement.Builder {
        void setName(String name);

        FunctionElement build();

        void setType(FunctionType type);

        void setVisibleAnnotations(List<Annotation> annotations);

        void setInvisibleAnnotations(List<Annotation> annotations);

        void setReturnVisibleTypeAnnotations(TypeAnnotationList returnVisibleTypeAnnotations);

        void setReturnInvisibleTypeAnnotations(TypeAnnotationList returnInvisibleTypeAnnotations);
    }

    static final class BuilderImpl extends InvokableElement.BuilderImpl implements Builder {
        private String name;

        BuilderImpl() {}

        @Override
        public void setName(String name) {
            this.name = Assert.checkNotNullParam("name", name);
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
        public void setVisibleAnnotations(List<Annotation> annotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setInvisibleAnnotations(List<Annotation> annotations) {
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
