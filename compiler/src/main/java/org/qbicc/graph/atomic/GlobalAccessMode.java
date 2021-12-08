package org.qbicc.graph.atomic;

/**
 * An access mode that applies to all of memory.  Only global access modes may be used in a general fence instruction.
 */
public sealed interface GlobalAccessMode extends AccessMode permits GlobalReadAccessMode, GlobalWriteAccessMode, PureFences {
    @Override
    default GlobalAccessMode combinedWith(AccessMode other) {
        return (GlobalAccessMode) AccessMode.super.combinedWith(other);
    }

    @Override
    default GlobalAccessMode getGlobalAccess() {
        return this;
    }
}
