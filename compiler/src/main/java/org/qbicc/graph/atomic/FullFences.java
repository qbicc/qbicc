package org.qbicc.graph.atomic;

enum FullFences implements GlobalReadWriteAccessMode, AtomicTraits {
    GlobalUnshared(GLOBAL),
    GlobalPlain(GLOBAL_ATOMIC),
    GlobalSeqCst(GLOBAL_SEQ_CST);

    private final int bits;

    FullFences(int bits) {
        this.bits = bits;
    }

    @Override
    public int getBits() {
        return bits;
    }
}
