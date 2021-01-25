package cc.quarkus.qcc.interpreter.impl;

import cc.quarkus.qcc.interpreter.VmClass;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.type.PhysicalObjectType;

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
