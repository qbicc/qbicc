package org.qbicc.type.definition.element;

import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.generic.MethodSignature;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class InitializerElement extends BasicElement implements ExecutableElement {
    final MethodBodyFactory methodBodyFactory;
    final int methodBodyFactoryIndex;
    volatile MethodBody previousMethodBody;
    volatile MethodBody methodBody;
    final int minimumLineNumber;
    final int maximumLineNumber;
    boolean inProgress;

    InitializerElement(BuilderImpl builder) {
        super(builder);
        this.methodBodyFactory = builder.methodBodyFactory;
        this.methodBodyFactoryIndex = builder.methodBodyFactoryIndex;
        this.minimumLineNumber = builder.minimumLineNumber;
        this.maximumLineNumber = builder.maximumLineNumber;
    }

    public boolean hasMethodBodyFactory() {
        return methodBodyFactory != null;
    }

    public boolean hasMethodBody() {
        return methodBody != null;
    }

    public MethodBody getPreviousMethodBody() {
        return previousMethodBody;
    }

    public MethodBody getMethodBody() {
        MethodBody methodBody = this.methodBody;
        if (methodBody == null) {
            throw new IllegalStateException("No method body is present on this element");
        }
        return methodBody;
    }

    public boolean tryCreateMethodBody() {
        MethodBody methodBody = this.methodBody;
        if (methodBody == null) {
            MethodBodyFactory factory = this.methodBodyFactory;
            if (factory != null) {
                synchronized (this) {
                    methodBody = this.methodBody;
                    if (methodBody == null) {
                        if (inProgress) {
                            return true;
                        }
                        inProgress = true;
                        try {
                            this.methodBody = previousMethodBody = factory.createMethodBody(methodBodyFactoryIndex, this);
                        } finally {
                            inProgress = false;
                        }
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public void replaceMethodBody(final MethodBody replacement) {
        MethodBody existing = this.methodBody;
        if (existing != null) {
            previousMethodBody = existing;
        }
        this.methodBody = replacement;
    }

    public FunctionType getType() {
        ClassContext classContext = getEnclosingType().getContext();
        TypeSystem ts = classContext.getTypeSystem();
        return ts.getFunctionType(ts.getVoidType());
    }

    public MethodDescriptor getDescriptor() {
        return MethodDescriptor.VOID_METHOD_DESCRIPTOR;
    }

    public MethodSignature getSignature() {
        return MethodSignature.VOID_METHOD_SIGNATURE;
    }

    public int getMinimumLineNumber() {
        return minimumLineNumber;
    }

    public int getMaximumLineNumber() {
        return maximumLineNumber;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String toString() {
        return "Initializer(" + getEnclosingType().getInternalName() + ")";
    }

    public interface Builder extends BasicElement.Builder, ExecutableElement.Builder {
        void setMethodBodyFactory(final MethodBodyFactory factory, final int index);

        void setMinimumLineNumber(int minimumLineNumber);

        void setMaximumLineNumber(int maximumLineNumber);

        InitializerElement build();

        interface Delegating extends BasicElement.Builder.Delegating, ExecutableElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setMinimumLineNumber(int minimumLineNumber) {
                getDelegate().setMinimumLineNumber(minimumLineNumber);
            }

            @Override
            default void setMaximumLineNumber(int maximumLineNumber) {
                getDelegate().setMaximumLineNumber(maximumLineNumber);
            }

            @Override
            default void setMethodBodyFactory(MethodBodyFactory factory, int index) {
                getDelegate().setMethodBodyFactory(factory, index);
            }

            @Override
            default InitializerElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends BasicElement.BuilderImpl implements Builder {
        MethodBodyFactory methodBodyFactory;
        int methodBodyFactoryIndex;
        int minimumLineNumber = 1;
        int maximumLineNumber = 1;

        BuilderImpl() {}

        public void setMethodBodyFactory(final MethodBodyFactory factory, final int index) {
            this.methodBodyFactory = Assert.checkNotNullParam("factory", factory);
            this.methodBodyFactoryIndex = index;
        }

        public void setMinimumLineNumber(int minimumLineNumber) {
            this.minimumLineNumber = minimumLineNumber;
        }

        public void setMaximumLineNumber(int maximumLineNumber) {
            this.maximumLineNumber = maximumLineNumber;
        }

        public InitializerElement build() {
            return new InitializerElement(this);
        }
    }
}
