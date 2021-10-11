package org.qbicc.interpreter;

import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceArrayObjectType;

/**
 *
 */
public interface VmReferenceArrayClass extends VmArrayClass {
    @Override
    ReferenceArrayObjectType getInstanceObjectType();

    ObjectType getLeafTypeId();

    @Override
    VmReferenceArray newInstance(int length) throws Thrown;
}
