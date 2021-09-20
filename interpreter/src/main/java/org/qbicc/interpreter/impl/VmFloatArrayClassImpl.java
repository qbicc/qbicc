package org.qbicc.interpreter.impl;

import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmFloatArrayClassImpl extends VmArrayClassImpl {
    VmFloatArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
    }

    @Override
    public String getName() {
        return "[F";
    }

    @Override
    public VmFloatArrayImpl newInstance(int length) {
        return new VmFloatArrayImpl(getVm(), length);
    }

    @Override
    public PrimitiveArrayObjectType getInstanceObjectType() {
        return getVm().getCompilationContext().getTypeSystem().getFloat32Type().getPrimitiveArrayObjectType();
    }
}
