package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 *
 */
class VmPrimitiveClassImpl extends VmClassImpl implements VmPrimitiveClass {
    private final LoadedTypeDefinition arrayTypeDefinition;
    private final String simpleName;

    VmPrimitiveClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, LoadedTypeDefinition arrayTypeDefinition, String simpleName) {
        super(vmImpl, classClass, 0);
        this.arrayTypeDefinition = arrayTypeDefinition;
        this.simpleName = simpleName;
    }

    @Override
    VmObjectImpl newInstance() {
        throw new UnsupportedOperationException("Cannot construct a primitive instance");
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    VmArrayClassImpl getArrayClass() {
        return (VmArrayClassImpl) arrayTypeDefinition.getVmClass();
    }

    @Override
    void postConstruct(VmImpl vm) {
        postConstruct(simpleName, vm);
    }

    public ObjectType getInstanceObjectType() {
        throw new UnsupportedOperationException("No instance object type for primitive classes");
    }
}
