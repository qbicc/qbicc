package org.qbicc.object;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

/**
 * An object which will be emitted to the final program.
 */
public abstract class ProgramObject {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(),
        "pointer", VarHandle.class, ProgramObject.class, ProgramObjectPointer.class);

    final String name;
    final ValueType valueType;
    @SuppressWarnings("unused") // VarHandle
    private final PointerType type;
    volatile Linkage linkage = Linkage.EXTERNAL;
    volatile ThreadLocalMode threadLocalMode;
    volatile ProgramObjectPointer pointer;

    ProgramObject(final String name, final ValueType valueType) {
        this.name = name;
        this.valueType = valueType;
        this.type = valueType.getPointer();
    }

    ProgramObject(final ProgramObject original) {
        this.name = original.getName();
        this.valueType = original.getValueType();
        this.type = original.getSymbolType();
        this.linkage = original.getLinkage();
        this.threadLocalMode = original.getThreadLocalMode();
    }

    public abstract ProgramModule getProgramModule();

    public String getName() {
        return name;
    }

    /**
     * Get the type of this symbol.  Symbols are addresses so they always have pointer type.
     *
     * @return the type of this symbol (not {@code null})
     */
    public final PointerType getSymbolType() {
        return this.type;
    }
    /**
     * Get the a pointer to this program object.  Convenience method which delegates to {@link ProgramObjectPointer#of}.
     *
     * @return the pointer
     */
    public ProgramObjectPointer getPointer() {
        return ProgramObjectPointer.of(this);
    }

    /**
     * Establish the pointer for this program object; intended only for use by {@link ProgramObjectPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see ProgramObjectPointer#of
     */
    public ProgramObjectPointer getOrCreatePointer(Function<ProgramObject, ProgramObjectPointer> factory) {
        ProgramObjectPointer pointer = this.pointer;
        if (pointer == null) {
            pointer = factory.apply(this);
            ProgramObjectPointer appearing = (ProgramObjectPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
    }

    public abstract Declaration getDeclaration();

    /**
     * Get the type of the value contained in this object.  This is the pointee of the symbol type,
     * but calling this method does not commit the program object to an address space.
     *
     * @return the value type (not {@code null})
     */
    public ValueType getValueType() {
        return valueType;
    }

    public <T extends ValueType> T getValueType(Class<T> expected) {
        return expected.cast(getValueType());
    }

    public Linkage getLinkage() {
        return linkage;
    }

    public void setLinkage(final Linkage linkage) {
        this.linkage = Assert.checkNotNullParam("linkage", linkage);
    }

    public ThreadLocalMode getThreadLocalMode() {
        return threadLocalMode;
    }

    public void setThreadLocalMode(ThreadLocalMode threadLocalMode) {
        this.threadLocalMode = threadLocalMode;
    }

}
