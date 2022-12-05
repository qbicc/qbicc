package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import org.qbicc.pointer.InstanceMethodPointer;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.util.ResolutionUtil;

/**
 * An instance method element.
 */
public final class InstanceMethodElement extends MethodElement {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, InstanceMethodElement.class, InstanceMethodPointer.class);
    @SuppressWarnings("unused") // pointerHandle
    private volatile InstanceMethodPointer pointer;

    InstanceMethodElement(BuilderImpl builder) {
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

    @Override
    public void setModifierFlags(int flags) {
        if ((flags & ClassFile.ACC_STATIC) != 0) {
            throw new IllegalArgumentException("Cannot make an instance element into a static element");
        }
        super.setModifierFlags(flags);
    }

    /**
     * Find and return the method that this method overrides, if any.
     * This is a simple check for debugging purposes only.
     *
     * @return the overridden method, or {@code null} if none
     */
    public InstanceMethodElement getOverridden() {
        LoadedTypeDefinition td = getEnclosingType().load();
        return (InstanceMethodElement) td.getSuperClass().resolveMethodElementVirtual(getName(), getDescriptor(), false);
    }

    /**
     * Establish the pointer for this method; intended only for use by {@link InstanceMethodPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see InstanceMethodPointer#of
     */
    public InstanceMethodPointer getOrCreateInstanceMethodPointer(Function<InstanceMethodElement, InstanceMethodPointer> factory) {
        InstanceMethodPointer pointer = this.pointer;
        if (pointer == null) {
            pointer = factory.apply(this);
            InstanceMethodPointer appearing = (InstanceMethodPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
    }
}
