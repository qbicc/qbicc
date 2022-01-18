package org.qbicc.interpreter;

import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;

/**
 *
 */
public interface VmArrayClass extends VmClass {
    VmArray newInstance(int length) throws Thrown;

    ArrayObjectType getInstanceObjectType();

    @Override
    ObjectType getInstanceObjectTypeId();

    @Override
    ArrayTypeDescriptor getDescriptor();
}
