package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;
import org.qbicc.pointer.FunctionPointer;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.util.ResolutionUtil;

/**
 * An element that represents some function.
 */
public final class FunctionElement extends InvokableElement implements NamedElement {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, FunctionElement.class, FunctionPointer.class);
    @SuppressWarnings("unused") // pointerHandle
    private volatile FunctionPointer pointer;
    private final String name;

    FunctionElement(final BuilderImpl builder) {
        super(builder);
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
        return ResolutionUtil.resolveFunctionType(getEnclosingType(), this, getDescriptor(), getSignature(), isVarargs());
    }

    /**
     * Establish the pointer for this function; intended only for use by {@link FunctionPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see FunctionPointer#of
     */
    public FunctionPointer getOrCreatePointer(Function<FunctionElement, FunctionPointer> factory) {
        FunctionPointer pointer = this.pointer;
        if (pointer == null) {
            pointer = factory.apply(this);
            FunctionPointer appearing = (FunctionPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
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

        void setVisibleTypeAnnotations(TypeAnnotationList returnVisibleTypeAnnotations);

        void setInvisibleTypeAnnotations(TypeAnnotationList returnInvisibleTypeAnnotations);

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
            default void setVisibleTypeAnnotations(final TypeAnnotationList annotations) {
                getDelegate().setVisibleTypeAnnotations(annotations);
            }

            @Override
            default void setInvisibleTypeAnnotations(final TypeAnnotationList annotations) {
                getDelegate().setInvisibleTypeAnnotations(annotations);
            }
        }
    }

    static final class BuilderImpl extends InvokableElement.BuilderImpl implements Builder {
        private final String name;

        BuilderImpl(String name, MethodDescriptor descriptor, int index) {
            super(descriptor, index);
            this.name = name;
            this.safePointBehavior = SafePointBehavior.ENTER;
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
        public void setVisibleTypeAnnotations(TypeAnnotationList returnVisibleTypeAnnotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

        @Override
        public void setInvisibleTypeAnnotations(TypeAnnotationList returnInvisibleTypeAnnotations) {
            throw new UnsupportedOperationException("Functions do not support annotations");
        }

    }
}
