package org.qbicc.graph.atomic;

/**
 * An access mode that is valid for both read and global operations.
 */
public sealed interface GlobalReadAccessMode extends GlobalAccessMode, ReadAccessMode permits GlobalReadWriteAccessMode, ReadFences {
    @Override
    default GlobalReadAccessMode getReadAccess() {
        return this;
    }

    @Override
    GlobalReadWriteAccessMode getWriteAccess();

    @Override
    default GlobalReadAccessMode getGlobalAccess() {
        return this;
    }
}
