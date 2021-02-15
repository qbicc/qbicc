package org.qbicc.interpreter.impl;

import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmDoubleArrayClassImpl extends VmArrayClassImpl {
    VmDoubleArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    public String getName() {
        return "[D";
    }

    @Override
    public VmDoubleArrayImpl newInstance(int length) {
        return new VmDoubleArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getFloat64Type().getPrimitiveArrayObjectType();
    }
}
