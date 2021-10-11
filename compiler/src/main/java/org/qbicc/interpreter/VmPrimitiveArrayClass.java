package org.qbicc.interpreter;

import org.qbicc.type.PrimitiveArrayObjectType;

/**
 *
 */
public interface VmPrimitiveArrayClass extends VmArrayClass {
    @Override
    PrimitiveArrayObjectType getInstanceObjectType();
}
