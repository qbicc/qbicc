package org.qbicc.machine.llvm;

/**
 * A struct type.
 */
public interface StructType extends LLValue {
    /**
     * Add a member to this struct type.
     *
     * @param type the type to add
     * @return this struct type
     */
    StructType member(LLValue type, String name);
}
