package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.util.ResolutionUtil;

/**
 * A static method element.
 */
public final class StaticMethodElement extends MethodElement {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, StaticMethodElement.class, StaticMethodPointer.class);
    @SuppressWarnings("unused") // pointerHandle
    private volatile StaticMethodPointer pointer;

    StaticMethodElement() {
        super();
    }

    StaticMethodElement(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public StaticMethodType getType() {
        return (StaticMethodType) super.getType();
    }

    @Override
    StaticMethodType computeType() {
        return ResolutionUtil.resolveStaticMethodType(getEnclosingType(), this, getDescriptor(), getSignature());
    }

    @Override
    public void clearModifierFlags(int flags) {
        if ((flags & ClassFile.ACC_STATIC) != 0) {
            throw new IllegalArgumentException("Cannot make a static element into an instance element");
        }
        super.clearModifierFlags(flags);
    }

    /**
     * Establish the pointer for this method; intended only for use by {@link StaticMethodPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see StaticMethodPointer#of
     */
    public StaticMethodPointer getOrCreateStaticMethodPointer(Function<StaticMethodElement, StaticMethodPointer> factory) {
        StaticMethodPointer pointer = this.pointer;
        if (pointer == null) {
            pointer = factory.apply(this);
            StaticMethodPointer appearing = (StaticMethodPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
    }
}
