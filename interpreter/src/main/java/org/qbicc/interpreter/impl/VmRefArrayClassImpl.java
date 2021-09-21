package org.qbicc.interpreter.impl;

import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmRefArrayClassImpl extends VmArrayClassImpl {
    private final String name;

    VmRefArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
        name = elementType instanceof VmArrayClassImpl ? "[" + elementType.getName() : "[" + elementType.getName() + ";";
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public ReferenceArrayObjectType getInstanceObjectType() {
        return getElementType().getObjectType().getReferenceArrayObject();
    }

    @Override
    public ClassObjectType getInstanceObjectTypeId() {
        return CoreClasses.get(getVmClass().getVm().getCompilationContext()).getReferenceArrayTypeDefinition().getClassType();
    }

    @Override
    public VmRefArrayImpl  newInstance(int length) {
        return new VmRefArrayImpl(this, length);
    }
}
