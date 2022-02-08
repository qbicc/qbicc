package org.qbicc.interpreter;

import org.qbicc.type.ArrayObjectType;

/**
 *
 */
public interface VmArray extends VmObject {
    int getLength();

    long getArrayElementOffset(int index);

    ArrayObjectType getObjectType();

    @Override
    VmArrayClass getVmClass();
}
