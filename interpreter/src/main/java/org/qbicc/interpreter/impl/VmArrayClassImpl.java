package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmArrayClass;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
abstract class VmArrayClassImpl extends VmClassImpl implements VmArrayClass {
    private final VmClassImpl elementType;
    private final String simpleName;

    VmArrayClassImpl(final VmImpl vm, final VmClassClassImpl classClass, final LoadedTypeDefinition classDef, final VmClassImpl elementType, final VmObjectImpl protectionDomain) {
        super(vm, classClass, classDef, protectionDomain);
        this.elementType = elementType;
        simpleName = elementType.getSimpleName() + "[]";
    }

    VmClassImpl getElementType() {
        return elementType;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    VmObjectImpl newInstance() {
        throw new UnsupportedOperationException("Cannot construct an array without a length");
    }

    TypeDescriptor getDescriptor() {
        return ArrayTypeDescriptor.of(getTypeDefinition().getContext(), elementType.getDescriptor());
    }

    @Override
    public abstract ArrayObjectType getInstanceObjectType();

    @Override
    public abstract VmArrayImpl newInstance(int length);
}
