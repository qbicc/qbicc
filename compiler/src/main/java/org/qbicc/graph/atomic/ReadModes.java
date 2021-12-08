package org.qbicc.graph.atomic;

enum ReadModes implements ReadAccessMode, AtomicTraits {
    SingleAcquire,
    ;

    @Override
    public ReadWriteAccessMode getWriteAccess() {
        return ReadWriteModes.SingleSeqCst;
    }

    @Override
    public GlobalReadAccessMode getGlobalAccess() {
        return ReadFences.GlobalAcquire;
    }

    @Override
    public int getBits() {
        return SINGLE_ACQUIRE;
    }
}
