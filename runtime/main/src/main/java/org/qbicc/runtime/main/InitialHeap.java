package org.qbicc.runtime.main;

class InitialHeap {

    // The body of this method will be replaced by the compiler to return the actual heap
    private static byte[] getInitialHeap() {
        return new byte[0];
    }

    // The body of this method will be replaced by the compiler to return the actual relocation offset array
    private static int[] getInitialHeapRelocations() {
        return new int[0];
    }

    static void relocatePointers() {
        byte[] theHeap = getInitialHeap();
        int[] relocs = getInitialHeapRelocations();
        for (int i=0; i<relocs.length; i++) {
            theHeap[relocs[i]] += 16; // TODO: REAL VALUE HERE and some evil casting so we are writing it as-if it was a pointer at this slot.
        }
    }
}
