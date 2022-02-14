package org.qbicc.interpreter;

import org.qbicc.type.ArrayObjectType;

/**
 *
 */
public interface VmArray extends VmObject {
    int getLength();

    ArrayObjectType getObjectType();

    @Override
    VmArrayClass getVmClass();

    /**
     * Get the raw array portion of this object.
     *
     * @return the raw array
     */
    Object getArray();
}
