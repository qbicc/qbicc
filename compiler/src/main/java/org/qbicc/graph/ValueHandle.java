package org.qbicc.graph;

import org.qbicc.type.ValueType;

/**
 * A handle expression for some thing which is addressable at run time (i.e. can be read from and/or written to).
 */
public interface ValueHandle extends Unschedulable {
    /**
     * Get the type of the referred value.
     *
     * @return the referred value type
     */
    ValueType getValueType();

    /**
     * Determine whether this handle is writable.
     *
     * @return {@code true} if the handle is writable, {@code false} otherwise
     */
    default boolean isWritable() {
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
