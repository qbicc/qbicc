package org.qbicc.interpreter;

import org.qbicc.type.ReferenceArrayObjectType;

/**
 *
 */
public interface VmReferenceArray extends VmArray {
    @Override
    ReferenceArrayObjectType getObjectType();
}
