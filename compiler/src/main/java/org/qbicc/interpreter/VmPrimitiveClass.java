package org.qbicc.interpreter;

import org.qbicc.type.descriptor.BaseTypeDescriptor;

public interface VmPrimitiveClass extends VmClass {
    @Override
    BaseTypeDescriptor getDescriptor();
}
