package org.qbicc.graph;

import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

/**
 * A handle expression for some thing which is addressable at run time (i.e. can be read from and/or written to).
 */
public interface ValueHandle extends Unschedulable {
    /**
     * Get the type that a pointer to the referred value would have.
     *
     * @return the referred pointer type
     */
    PointerType getPointerType();

    /**
     * Get the type that the referred value would have. Equivalent to {@code getPointerType().getPointeeType()}.
     *
     * @return the referred value type
     */
    default ValueType getValueType() {
        return getPointerType().getPointeeType();
    }

    /**
     * Determine whether this handle is writable.
     *
     * @return {@code true} if the handle is writable, {@code false} otherwise
     */
    default boolean isWritable() {
        return true;
    }

    /**
     * Determine whether this handle is readable.
     *
     * @return {@code true} if the handle is readable, {@code false} otherwise
     */
    default boolean isReadable() {
        return true;
    }

    /**
     * Get the detected access mode for this handle.
     *
     * @return the detected access mode for this handle (must not be {@code null})
     */
    MemoryAtomicityMode getDetectedMode();

    <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param);
}
