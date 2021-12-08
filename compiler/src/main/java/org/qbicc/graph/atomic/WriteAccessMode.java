package org.qbicc.graph.atomic;

/**
 * An access mode that is valid for write operations.
 */
public sealed interface WriteAccessMode extends AccessMode permits GlobalWriteAccessMode, ReadWriteAccessMode, WriteModes {
    @Override
    ReadWriteAccessMode getReadAccess();

    @Override
    default WriteAccessMode getWriteAccess() {
        return this;
    }

    @Override
    GlobalWriteAccessMode getGlobalAccess();
}
