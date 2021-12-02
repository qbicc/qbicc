package org.qbicc.graph.atomic;

enum ReadWriteModes implements ReadWriteAccessMode, AtomicTraits {
    SingleUnshared(0) {
        @Override
        public GlobalReadWriteAccessMode getGlobalAccess() {
            return FullFences.GlobalUnshared;
        }
    },
    SinglePlain(SINGLE_ATOMIC) {
        @Override
        public GlobalReadWriteAccessMode getGlobalAccess() {
            return FullFences.GlobalPlain;
        }
    },
    SingleOpaque(SINGLE_OPAQUE),
    SingleSeqCst(SINGLE_SEQ_CST),
    ;

    private final int bits;

    ReadWriteModes(int bits) {
        this.bits = bits;
    }

    @Override
    public GlobalReadWriteAccessMode getGlobalAccess() {
        return FullFences.GlobalSeqCst;
    }

    @Override
    public int getBits() {
        return bits;
    }
}
