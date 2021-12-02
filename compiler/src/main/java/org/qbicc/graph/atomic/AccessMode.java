package org.qbicc.graph.atomic;

/**
 * A memory access mode. Not all access modes are valid for all operations; this is enforced by type. Unless an
 * access mode is global, it is only guaranteed to apply to memory directly given in the operation.
 * <p>
 * These interfaces exist for type safety and are not arbitrarily implementable.
 *
 * @see AccessModes
 */
public sealed interface AccessMode permits GlobalAccessMode, PureModes, ReadAccessMode, WriteAccessMode {

    /**
     * Determine whether this mode completely includes the given mode.
     *
     * @param other the mode that may be included (must not be {@code null})
     * @return {@code true} if the other mode is fully included in this mode, or {@code false} otherwise
     */
    default boolean includes(AccessMode other) {
        int myBits = ((AtomicTraits) this).getBits();
        int theirBits = ((AtomicTraits) other).getBits();
        return (myBits | theirBits) == myBits;
    }

    /**
     * Determine whether this mode overlaps with the given mode.
     *
     * @param other the mode that may overlap (must not be {@code null})
     * @return {@code true} if the other mode overlaps in any way with this mode, or {@code false} otherwise
     */
    default boolean overlapsWith(AccessMode other) {
        int myBits = ((AtomicTraits) this).getBits();
        int theirBits = ((AtomicTraits) other).getBits();
        return (myBits & theirBits) != 0;
    }

    /**
     * Determine whether this mode provides atomicity at the value level.  Non-atomic operations may allow safe misaligned
     * access on some CPU architectures.
     *
     * @return {@code true} if the mode provides atomicity, or {@code false} otherwise
     */
    default boolean isAtomic() {
        return (((AtomicTraits) this).getBits() & AtomicTraits.SINGLE_ATOMIC) != 0;
    }

    /**
     * Get the weakest access mode that is equal to or stronger than both this access mode and the given access mode.
     * The returned mode is not guaranteed to be usable in any particular context without being transformed by
     * one of {@link #getReadAccess()}, {@link #getWriteAccess()}, and/or {@link #getGlobalAccess()}.
     *
     * @param other the other access mode (must not be {@code null})
     * @return the combined access mode (not {@code null})
     */
    default AccessMode combinedWith(AccessMode other) {
        int myBits = ((AtomicTraits) this).getBits();
        int theirBits = ((AtomicTraits) other).getBits();
        return AtomicTraits.fromBits(myBits | theirBits);
    }

    /**
     * Get the weakest access mode that is equal to or stronger than both this access mode and the given access mode.
     *
     * @param other the other access mode (must not be {@code null})
     * @return the combined access mode (not {@code null})
     */
    default GlobalAccessMode combinedWith(GlobalAccessMode other) {
        return (GlobalAccessMode) combinedWith((AccessMode) other);
    }

    /**
     * Get the mode which is equal to or stronger than this mode which can be used in read operations.
     *
     * @return the read access mode (not {@code null})
     */
    ReadAccessMode getReadAccess();

    /**
     * Get the mode which is equal to or stronger than this mode which can be used in write operations.
     *
     * @return the write access mode (not {@code null})
     */
    WriteAccessMode getWriteAccess();

    /**
     * Get the mode which is equal to or stronger than this mode which can be used in global memory operations.
     *
     * @return the global access mode (not {@code null})
     */
    GlobalAccessMode getGlobalAccess();
}
