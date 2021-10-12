package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmPrimitiveArrayClass;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmCharArrayClassImpl extends VmArrayClassImpl implements VmPrimitiveArrayClass {
    VmCharArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    void postConstruct(VmImpl vm) {
        postConstruct(getName(), vm);
    }

    @Override
    public String getName() {
        return "[C";
    }

    @Override
    public VmCharArrayImpl newInstance(int length) {
        return new VmCharArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType  getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getUnsignedInteger16Type().getPrimitiveArrayObjectType();
    }

    @Override
    public ObjectType getInstanceObjectTypeId() {
        return CoreClasses.get(getVmClass().getVm().getCompilationContext()).getCharArrayTypeDefinition().getClassType();
    }
}
