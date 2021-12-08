package org.qbicc.graph.atomic;

import static org.qbicc.graph.atomic.AccessModes.*;

/**
 * A set of bits used to represent the set of capabilities that an atomic operation may have, in order to determine which
 * operations include which other operations.
 */
sealed interface AtomicTraits permits FullFences, PureFences, PureModes, ReadFences, ReadModes, ReadWriteModes, WriteFences, WriteModes {
    int getBits();

    int SINGLE_ATOMIC = 1 << 0;
    int SINGLE_OPAQUE = SINGLE_ATOMIC | 1 << 1;
    int SINGLE_ACQUIRE = SINGLE_OPAQUE | 1 << 2;
    int SINGLE_RELEASE = SINGLE_OPAQUE | 1 << 3;
    int SINGLE_ACQ_REL = SINGLE_ACQUIRE | SINGLE_RELEASE;
    int SINGLE_SEQ_CST = SINGLE_ACQ_REL | 1 << 4;

    int GLOBAL = 1 << 5;
    int GLOBAL_ATOMIC = GLOBAL | SINGLE_ATOMIC;
    int GLOBAL_LOAD_LOAD = GLOBAL | 1 << 6;
    int GLOBAL_LOAD_STORE = GLOBAL | 1 << 7;
    int GLOBAL_STORE_STORE = GLOBAL | 1 << 8;
    int GLOBAL_STORE_LOAD = GLOBAL | 1 << 9;
    int GLOBAL_ACQUIRE = GLOBAL_LOAD_LOAD | GLOBAL_LOAD_STORE | SINGLE_ACQUIRE;
    int GLOBAL_RELEASE = GLOBAL_STORE_STORE | GLOBAL_LOAD_STORE | SINGLE_RELEASE;
    int GLOBAL_ACQ_REL = GLOBAL_ACQUIRE | GLOBAL_RELEASE;
    int GLOBAL_SEQ_CST = GLOBAL_ACQ_REL | GLOBAL_STORE_LOAD | SINGLE_SEQ_CST;

    static boolean matches(int bits, int test) {
        return (bits & test) == bits;
    }

    static AccessMode fromBits(int bits) {
        // find the most minimal mode which matches all of the given bits
        if (matches(bits, 0)) {
            return SingleUnshared;
        } else if (matches(bits, SINGLE_ATOMIC)) {
            return SinglePlain;
        } else if (matches(bits, SINGLE_OPAQUE)) {
            return SingleOpaque;
        } else if (matches(bits, SINGLE_ACQUIRE)) {
            return SingleAcquire;
        } else if (matches(bits, SINGLE_RELEASE)) {
            return SingleRelease;
        } else if (matches(bits, SINGLE_ACQ_REL)) {
            return SingleAcqRel;
        } else if (matches(bits, SINGLE_SEQ_CST)) {
            return SingleSeqCst;
        } else if (matches(bits, GLOBAL)) {
            return GlobalUnshared;
        } else if (matches(bits, GLOBAL_ATOMIC)) {
            return GlobalPlain;
        } else if (matches(bits, GLOBAL_LOAD_LOAD)) {
            return GlobalLoadLoad;
        } else if (matches(bits, GLOBAL_LOAD_STORE)) {
            return GlobalLoadStore;
        } else if (matches(bits, GLOBAL_STORE_STORE)) {
            return GlobalStoreStore;
        } else if (matches(bits, GLOBAL_STORE_LOAD)) {
            return GlobalStoreLoad;
        } else if (matches(bits, GLOBAL_ACQUIRE)) {
            return GlobalAcquire;
        } else if (matches(bits, GLOBAL_RELEASE)) {
            return GlobalRelease;
        } else if (matches(bits, GLOBAL_ACQ_REL)) {
            return GlobalAcqRel;
        } else if (matches(bits, GLOBAL_SEQ_CST)) {
            return GlobalSeqCst;
        } else {
            throw new IllegalArgumentException("Invalid access mode");
        }
    }
}
