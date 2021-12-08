package org.qbicc.graph.atomic;

enum PureFences implements GlobalAccessMode, AtomicTraits {
    GlobalLoadLoad(GLOBAL_LOAD_LOAD) {
        @Override
        public ReadAccessMode getReadAccess() {
            return ReadFences.GlobalAcquire;
        }
    },
    GlobalLoadStore(GLOBAL_LOAD_STORE) {
        @Override
        public ReadAccessMode getReadAccess() {
            return ReadFences.GlobalAcquire;
        }

        @Override
        public WriteAccessMode getWriteAccess() {
            return WriteFences.GlobalRelease;
        }
    },
    GlobalStoreStore(GLOBAL_STORE_STORE) {
        @Override
        public WriteAccessMode getWriteAccess() {
            return WriteFences.GlobalRelease;
        }
    },
    GlobalStoreLoad(GLOBAL_STORE_LOAD),

    GlobalAcqRel(GLOBAL_ACQ_REL),
    ;

    private final int bits;

    PureFences(int bits) {
        this.bits = bits;
    }

    @Override
    public ReadAccessMode getReadAccess() {
        return FullFences.GlobalSeqCst;
    }

    @Override
    public WriteAccessMode getWriteAccess() {
        return FullFences.GlobalSeqCst;
    }

    @Override
    public int getBits() {
        return bits;
    }
}
