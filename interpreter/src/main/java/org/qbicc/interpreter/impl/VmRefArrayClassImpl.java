package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmReferenceArrayClass;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
final class VmRefArrayClassImpl extends VmArrayClassImpl implements VmReferenceArrayClass {
    private final String name;

    VmRefArrayClassImpl(VmImpl vm, VmClassClassImpl classClass, LoadedTypeDefinition classDef, VmClassImpl elementType) {
        super(vm, classClass, classDef, elementType, null);
        name = elementType instanceof VmArrayClassImpl ? "[" + elementType.getName() : "[" + elementType.getName() + ";";
    }

    @Override
    void postConstruct(VmImpl vm) {
        postConstruct(getName(), vm);
    }

    @Override
    void initVmClass() {
        // no operation
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public ReferenceArrayObjectType getInstanceObjectType() {
        return getElementType().getInstanceObjectType().getReferenceArrayObject();
    }

    @Override
    public ObjectType getLeafTypeId() {
        VmClassImpl elementType = getElementType();
        while (elementType instanceof VmRefArrayClassImpl) {
            elementType = ((VmRefArrayClassImpl) elementType).getElementType();
        }
        return elementType.getInstanceObjectTypeId();
    }

    @Override
    public ObjectType getInstanceObjectTypeId() {
        return CoreClasses.get(getVmClass().getVm().getCompilationContext()).getReferenceArrayTypeDefinition().getClassType();
    }

    @Override
    public VmRefArrayImpl  newInstance(int length) {
        return new VmRefArrayImpl(this, length);
    }
}
