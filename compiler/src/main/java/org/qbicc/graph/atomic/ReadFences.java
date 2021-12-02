package org.qbicc.graph.atomic;

enum ReadFences implements GlobalReadAccessMode, AtomicTraits {
    GlobalAcquire,
    ;

    @Override
    public GlobalReadWriteAccessMode getWriteAccess() {
        return FullFences.GlobalSeqCst;
    }

    @Override
    public int getBits() {
        return GLOBAL_ACQUIRE;
    }
}
