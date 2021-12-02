package org.qbicc.graph.atomic;

enum WriteModes implements WriteAccessMode, AtomicTraits {
    SingleRelease,
    ;

    @Override
    public GlobalWriteAccessMode getGlobalAccess() {
        return WriteFences.GlobalRelease;
    }

    @Override
    public ReadWriteAccessMode getReadAccess() {
        return ReadWriteModes.SingleSeqCst;
    }

    @Override
    public int getBits() {
        return SINGLE_RELEASE;
    }
}
