package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import org.qbicc.pointer.ConstructorPointer;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.util.ResolutionUtil;

/**
 *
 */
public final class ConstructorElement extends InvokableElement {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, ConstructorElement.class, ConstructorPointer.class);
    @SuppressWarnings("unused") // pointerHandle
    private volatile ConstructorPointer pointer;
    public static final ConstructorElement[] NO_CONSTRUCTORS = new ConstructorElement[0];

    ConstructorElement(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public InstanceMethodType getType() {
        return (InstanceMethodType) super.getType();
    }

    @Override
    InstanceMethodType computeType() {
        return ResolutionUtil.resolveInstanceMethodType(getEnclosingType(), this, getDescriptor(), getSignature());
    }

    /**
     * Establish the pointer for this constructor; intended only for use by {@link ConstructorPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see ConstructorPointer#of
     */
    public ConstructorPointer getOrCreatePointer(Function<ConstructorElement, ConstructorPointer> factory) {
        ConstructorPointer pointer = this.pointer;
        if (pointer == null) {
            pointer = factory.apply(this);
            ConstructorPointer appearing = (ConstructorPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public String toString() {
        TypeDescriptor descriptor = getEnclosingType().getDescriptor();
        if (descriptor instanceof ClassTypeDescriptor ctd) {
            final String packageName = ctd.getPackageName();
            if (packageName.isEmpty()) {
                return ctd.getClassName() + getDescriptor();
            }
            return packageName + "." + ctd.getClassName() + getDescriptor();
        } else if (descriptor instanceof BaseTypeDescriptor btd) {
            return btd.getFullName() + getDescriptor();
        } else {
            throw new IllegalStateException();
        }
    }

    public static Builder builder(MethodDescriptor descriptor, int index) {
        return new BuilderImpl(descriptor, index);
    }

    public interface Builder extends InvokableElement.Builder {
        ConstructorElement build();

        interface Delegating extends InvokableElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default ConstructorElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends InvokableElement.BuilderImpl implements Builder {
        BuilderImpl(MethodDescriptor descriptor, int index) {
            super(descriptor, index);
        }

        public ConstructorElement build() {
            return new ConstructorElement(this);
        }
    }
}
