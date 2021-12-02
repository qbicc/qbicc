package org.qbicc.graph.atomic;

/**
 *
 */
enum WriteFences implements GlobalWriteAccessMode, AtomicTraits {
    GlobalRelease,
    ;

    @Override
    public GlobalReadWriteAccessMode getReadAccess() {
        return FullFences.GlobalSeqCst;
    }

    @Override
    public int getBits() {
        return GLOBAL_RELEASE;
    }
}
