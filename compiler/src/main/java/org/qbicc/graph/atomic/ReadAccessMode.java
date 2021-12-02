package org.qbicc.graph.atomic;

/**
 * An access mode that is valid for read operations.
 */
public sealed interface ReadAccessMode extends AccessMode permits GlobalReadAccessMode, ReadModes, ReadWriteAccessMode {
    @Override
    default ReadAccessMode getReadAccess() {
        return this;
    }

    @Override
    ReadWriteAccessMode getWriteAccess();

    @Override
    GlobalReadAccessMode getGlobalAccess();
}
