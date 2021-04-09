package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.PhysicalObjectType;

abstract class VmObjectBase implements VmObject {
    final VmClass clazz;

    VmObjectBase(final VmClass clazz) {
        this.clazz = clazz;
    }

    public VmClass getVmClass() {
        return clazz;
    }

    public PhysicalObjectType getObjectType() {
        return clazz.getTypeDefinition().getClassType();
    }
}
