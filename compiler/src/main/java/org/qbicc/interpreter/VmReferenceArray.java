package org.qbicc.interpreter;

import org.qbicc.type.ReferenceArrayObjectType;

/**
 *
 */
public interface VmReferenceArray extends VmArray {
    @Override
    ReferenceArrayObjectType getObjectType();

    /**
     * Directly store a value into the array.
     *
     * @param index the array index
     * @param value the value to store
     */
    void store(int index, VmObject value);
}
