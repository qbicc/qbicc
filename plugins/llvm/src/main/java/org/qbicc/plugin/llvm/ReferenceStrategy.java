package org.qbicc.plugin.llvm;

/**
 * The reference-encoding strategy used by the LLVM plugin.
 */
public enum ReferenceStrategy {
    /**
     * References are pointers.
     */
    POINTER,
    /**
     * References are pointers in address space number 1.
     */
    POINTER_AS1,
    ;
}
