package org.qbicc.interpreter;

import org.qbicc.type.ArrayObjectType;

/**
 *
 */
public interface VmArray extends VmObject {
    int getLength();

    int getArrayElementOffset(int index);

    ArrayObjectType getObjectType();
}
