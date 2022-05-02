package org.qbicc.runtime.main;

@SuppressWarnings("unused")
public class InitialHeap {
    // All of the interned String carried over from build time.
    // After the runtime String intern table is initialized, this field will
    // be set to null to allow the String[] object to be garbage collected.
    public static String[] internedStrings;

    static class ClassSection {}
    static class ObjectSection {}
}
