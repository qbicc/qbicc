package org.qbicc.interpreter.impl;

import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmByteArrayClassImpl extends VmArrayClassImpl {
    VmByteArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    public String getName() {
        return "[B";
    }

    @Override
    public VmByteArrayImpl newInstance(int length) {
        return new VmByteArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType  getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getSignedInteger8Type().getPrimitiveArrayObjectType();
    }
}
