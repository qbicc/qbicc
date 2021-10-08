package org.qbicc.interpreter.impl;

import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmShortArrayClassImpl extends VmArrayClassImpl {
    VmShortArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    public String getName() {
        return "[S";
    }

    @Override
    public VmShortArrayImpl newInstance(int length) {
        return new VmShortArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getSignedInteger16Type().getPrimitiveArrayObjectType();
    }

    @Override
    public ObjectType getInstanceObjectTypeId() {
        return CoreClasses.get(getVmClass().getVm().getCompilationContext()).getShortArrayTypeDefinition().getClassType();
    }
}
