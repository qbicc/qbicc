package org.qbicc.interpreter;

import org.qbicc.type.ArrayObjectType;

/**
 *
 */
public interface VmArrayClass extends VmClass {
    VmArray newInstance(int length) throws Thrown;

    ArrayObjectType getInstanceObjectType();
}
