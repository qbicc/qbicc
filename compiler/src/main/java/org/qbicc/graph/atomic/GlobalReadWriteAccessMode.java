package org.qbicc.graph.atomic;

/**
 * An access mode that is valid for read, write, and global operations.
 */
public sealed interface GlobalReadWriteAccessMode extends ReadWriteAccessMode, GlobalReadAccessMode, GlobalWriteAccessMode permits FullFences {
    @Override
    default GlobalReadWriteAccessMode getGlobalAccess() {
        return this;
    }

    @Override
    default GlobalReadWriteAccessMode getReadAccess() {
        return this;
    }

    @Override
    default GlobalReadWriteAccessMode getWriteAccess() {
        return this;
    }
}
