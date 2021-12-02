package org.qbicc.graph.atomic;

/**
 *
 */
enum PureModes implements AccessMode, AtomicTraits {
    SingleAcqRel,
    ;

    @Override
    public ReadAccessMode getReadAccess() {
        return ReadWriteModes.SingleSeqCst;
    }

    @Override
    public WriteAccessMode getWriteAccess() {
        return ReadWriteModes.SingleSeqCst;
    }

    @Override
    public GlobalAccessMode getGlobalAccess() {
        return PureFences.GlobalAcqRel;
    }

    @Override
    public int getBits() {
        return SINGLE_ACQ_REL;
    }
}
