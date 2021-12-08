package org.qbicc.graph.atomic;

/**
 * An access mode that is valid for both read and write operations.
 */
public sealed interface ReadWriteAccessMode extends ReadAccessMode, WriteAccessMode permits GlobalReadWriteAccessMode, ReadWriteModes {
    @Override
    GlobalReadWriteAccessMode getGlobalAccess();

    @Override
    default ReadWriteAccessMode getWriteAccess() {
        return this;
    }

    @Override
    default ReadWriteAccessMode getReadAccess() {
        return this;
    }
}
