package org.qbicc.interpreter.impl;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.VmPrimitiveClass;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.descriptor.BaseTypeDescriptor;

/**
 *
 */
class VmPrimitiveClassImpl extends VmClassImpl implements VmPrimitiveClass {
    private final LoadedTypeDefinition arrayTypeDefinition;
    private final String simpleName;
    private final BaseTypeDescriptor descriptor;

    VmPrimitiveClassImpl(VmImpl vmImpl, VmClassClassImpl classClass, LoadedTypeDefinition arrayTypeDefinition, String simpleName, BaseTypeDescriptor descriptor) {
        super(vmImpl, classClass, 0);
        this.arrayTypeDefinition = arrayTypeDefinition;
        this.simpleName = simpleName;
        this.descriptor = descriptor;
    }

    @Override
    VmObjectImpl newInstance() {
        throw new UnsupportedOperationException("Cannot construct a primitive instance");
    }

    @Override
    BaseTypeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String getName() {
        return simpleName;
    }

    @Override
    public VmArrayClassImpl getArrayClass() {
        return (VmArrayClassImpl) arrayTypeDefinition.getVmClass();
    }

    @Override
    void postConstruct(VmImpl vm) {
        postConstruct(simpleName, vm);
    }

    void setArrayClass(CompilationContext ctxt, VmArrayClassImpl arrayClazz) {
        // post-construct array type def
        int acfIdx = indexOf(CoreClasses.get(ctxt).getArrayClassField());
        getMemory().storeRef(acfIdx, arrayClazz, MemoryAtomicityMode.VOLATILE);
    }

    public ObjectType getInstanceObjectType() {
        throw new UnsupportedOperationException("No instance object type for primitive classes");
    }
}
