package org.qbicc.object;

/**
 *
 */
public enum Linkage {
    /**
     * Not visible outside of the current program module.
     */
    PRIVATE,
    /**
     * Symbol is visible as a local symbol in the object file; not linkable outside of the current program module.
     */
    INTERNAL,
    /**
     * External symbol is linked weakly.
     */
    WEAK,
    /**
     * Object goes into uninitialized ({@code bss}) section unless another definition overrides it; symbol is visible.
     */
    COMMON,
    /**
     * The object is externally visible.
     */
    EXTERNAL,
}
