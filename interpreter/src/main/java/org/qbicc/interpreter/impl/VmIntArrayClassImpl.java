package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmPrimitiveArrayClass;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmIntArrayClassImpl extends VmArrayClassImpl implements VmPrimitiveArrayClass {
    VmIntArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    void postConstruct(VmImpl vm) {
        postConstruct(getName(), vm);
    }

    @Override
    public String getName() {
        return "[I";
    }

    @Override
    public VmIntArrayImpl newInstance(int length) {
        return new VmIntArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getSignedInteger32Type().getPrimitiveArrayObjectType();
    }

    @Override
    public ObjectType getInstanceObjectTypeId() {
        return CoreClasses.get(getVmClass().getVm().getCompilationContext()).getIntArrayTypeDefinition().getClassType();
    }
}
