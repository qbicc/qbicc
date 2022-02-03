package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.MethodType;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.util.ResolutionUtil;

/**
 *
 */
public final class MethodElement extends InvokableElement implements NamedElement {
    public static final MethodElement[] NO_METHODS = new MethodElement[0];

    /**
     * Special marker method used in method searches.
     */
    public static final MethodElement NOT_FOUND = new MethodElement();
    /**
     * Special marker method used in method searches.
     */
    public static final MethodElement END_OF_SEARCH = new MethodElement();

    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, MethodElement.class, StaticMethodPointer.class);

    private final String name;
    private final AnnotationValue defaultValue;
    private volatile StaticMethodPointer pointer;

    MethodElement() {
        super();
        this.name = null;
        this.defaultValue = null;
    }

    MethodElement(BuilderImpl builder) {
        super(builder);
        this.name = builder.name;
        this.defaultValue = builder.defaultValue;
    }

    @Override
    public MethodType getType() {
        return (MethodType) super.getType();
    }

    @Override
    MethodType computeType() {
        return isStatic() ?
            ResolutionUtil.resolveStaticMethodType(getEnclosingType().getContext(), this, getDescriptor(), getSignature()) :
            ResolutionUtil.resolveInstanceMethodType(getEnclosingType().getContext(), getEnclosingType(), this, getDescriptor(), getSignature());
    }

    public String toString() {
        final String packageName = getEnclosingType().getDescriptor().getPackageName();
        if (packageName.isEmpty()) {
            return getEnclosingType().getDescriptor().getClassName()+"."+getName()+getDescriptor();
        }
        return packageName+"."+getEnclosingType().getDescriptor().getClassName()+"."+getName()+getDescriptor();
    }

    public String getName() {
        return name;
    }

    public AnnotationValue getDefaultValue() {
        return defaultValue;
    }

    public boolean isAbstract() {
        return hasAllModifiersOf(ClassFile.ACC_ABSTRACT);
    }

    public boolean isFinal() {
        return hasAllModifiersOf(ClassFile.ACC_FINAL);
    }

    public boolean isStatic() {
        return hasAllModifiersOf(ClassFile.ACC_STATIC);
    }

    public boolean isVirtual() {
        return hasNoModifiersOf(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC);
    }

    public boolean isNative() {
        return hasAllModifiersOf(ClassFile.ACC_NATIVE);
    }

    public boolean isSignaturePolymorphic() {
        return hasAllModifiersOf(ClassFile.I_ACC_SIGNATURE_POLYMORPHIC);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder(String name, MethodDescriptor descriptor, int index) {
        return new BuilderImpl(name, descriptor, index);
    }

    public boolean overrides(final MethodElement other) {
        // todo: account for access control cases
        return ! isStatic()
            && ! other.isStatic()
            && ! other.isFinal()
            && getDescriptor().equals(other.getDescriptor())
            && getName().equals(other.getName())
            && getEnclosingType().load().getType().isSubtypeOf(other.getEnclosingType().load().getType());
    }

    /**
     * Get the pointer to this (static) method.  Convenience method which delegates to {@link StaticMethodPointer#of}.
     *
     * @return the pointer
     * @throws IllegalArgumentException if this method is not static
     */
    public StaticMethodPointer getStaticMethodPointer() {
        return StaticMethodPointer.of(this);
    }

    /**
     * Establish the pointer for this method; intended only for use by {@link StaticMethodPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see StaticMethodPointer#of
     */
    public StaticMethodPointer getOrCreateStaticMethodPointer(Function<MethodElement, StaticMethodPointer> factory) {
        StaticMethodPointer pointer = this.pointer;
        if (pointer == null) {
            if (! isStatic()) {
                throw new IllegalArgumentException("Static pointer for instance method");
            }
            pointer = factory.apply(this);
            StaticMethodPointer appearing = (StaticMethodPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
    }


    public interface Builder extends InvokableElement.Builder, NamedElement.Builder {

        void setDefaultValue(AnnotationValue annotationValue);

        MethodElement build();

        interface Delegating extends InvokableElement.Builder.Delegating, NamedElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setDefaultValue(AnnotationValue annotationValue) {
                getDelegate().setDefaultValue(annotationValue);
            }

            @Override
            default MethodElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends InvokableElement.BuilderImpl implements Builder {
        final String name;
        AnnotationValue defaultValue;

        BuilderImpl(String name, MethodDescriptor descriptor, int index) {
            super(descriptor, index);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setDefaultValue(AnnotationValue annotationValue) {
            this.defaultValue = annotationValue;
        }

        public MethodElement build() {
            return new MethodElement(this);
        }
    }
}
