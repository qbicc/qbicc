package org.qbicc.runtime.main;

@SuppressWarnings("unused")
public class InitialHeap {
    // All of the interned String objects carried over from build time.
    // The array is sorted in "natural order" to enable Arrays.binarySearch
    // to be used at runtime by String.intern() for efficient lookup.
    public static String[] internedStrings;

    static class ClassSection {}
    static class InternedStringSection {}
    static class ObjectSection {}
}
