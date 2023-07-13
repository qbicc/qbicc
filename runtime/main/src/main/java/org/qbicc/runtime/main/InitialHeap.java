package org.qbicc.runtime.main;

@SuppressWarnings("unused")
public class InitialHeap {
    // All of the interned String objects carried over from build time.
    // The array is sorted in "natural order" to enable Arrays.binarySearch
    // to be used at runtime by String.intern() for efficient lookup.
    public static String[] internedStrings;

    // Tables to allow runtime mapping of a <ClassLoader, String> pair to a build-time loaded Class object.
    // Used to implement Class.forName0, ClassLoader.findBootstrapClass, and ClassLoader.findLoadedClass0
    // See VMHelpers.findLoaderClass
    static ClassLoader[] classLoaders;
    static String[][] classNames;  // sorted in natural order for Arrays.binarySearch
    static Class<?>[][] classes;   // bootstrapClasses[i].name == bootstrapClassNames[i]

    static class ClassSection {}
    static class InternedStringSection {}
    static class ObjectSection {}
    static class RefSection {}
}
