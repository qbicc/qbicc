package org.qbicc.graph.atomic;

/**
 * An access mode that is valid for both write and global operations.
 */
public sealed interface GlobalWriteAccessMode extends GlobalAccessMode, WriteAccessMode permits GlobalReadWriteAccessMode, WriteFences {
    @Override
    GlobalReadWriteAccessMode getReadAccess();

    @Override
    default GlobalWriteAccessMode getWriteAccess() {
        return this;
    }

    @Override
    default GlobalWriteAccessMode getGlobalAccess() {
        return this;
    }
}
