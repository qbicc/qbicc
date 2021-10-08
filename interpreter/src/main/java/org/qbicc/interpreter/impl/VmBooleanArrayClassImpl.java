package org.qbicc.interpreter.impl;

import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmBooleanArrayClassImpl extends VmArrayClassImpl {
    VmBooleanArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    public String getName() {
        return "[Z";
    }

    @Override
    public VmBooleanArrayImpl newInstance(int length) {
        return new VmBooleanArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getBooleanType().getPrimitiveArrayObjectType();
    }

    @Override
    public ObjectType getInstanceObjectTypeId() {
        return CoreClasses.get(getVmClass().getVm().getCompilationContext()).getBooleanArrayTypeDefinition().getClassType();
    }
}
