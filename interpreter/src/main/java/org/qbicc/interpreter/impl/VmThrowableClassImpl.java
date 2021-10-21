package org.qbicc.interpreter.impl;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
class VmThrowableClassImpl extends VmClassImpl implements VmThrowableClass {
    public VmThrowableClassImpl(final VmImpl vm, final LoadedTypeDefinition def, final VmObject protectionDomain) {
        super(vm, def, protectionDomain);
    }

    @Override
    VmThrowableImpl newInstance() {
        VmThrowableImpl throwable = new VmThrowableImpl(this);
        throwable.initializeDepth();
        return throwable;
    }

    @Override
    public VmThrowable newInstance(String message) {
        VmThrowableImpl throwable = new VmThrowableImpl(this);
        VmImpl vm = getVm();
        vm.manuallyInitialize(throwable);
        throwable.initializeDepth();
        LoadedTypeDefinition typeDefinition = getTypeDefinition();
        ClassContext context = typeDefinition.getContext();
        ClassTypeDescriptor jls = ClassTypeDescriptor.synthesize(context, "java/lang/String");
        int ci = typeDefinition.findConstructorIndex(MethodDescriptor.synthesize(context, BaseTypeDescriptor.V, List.of(jls)));
        if (ci == -1) {
            throw new IllegalArgumentException("Cannot construct exception with message (no matching ctor) for " + getName());
        }
        vm.invokeExact(typeDefinition.getConstructor(ci), throwable, List.of(vm.intern(message)));
        return throwable;
    }

    @Override
    public VmThrowable newInstance(VmThrowable cause) {
        VmThrowableImpl throwable = new VmThrowableImpl(this);
        VmImpl vm = getVm();
        vm.manuallyInitialize(throwable);
        throwable.initializeDepth();
        LoadedTypeDefinition typeDefinition = getTypeDefinition();
        ClassContext context = typeDefinition.getContext();
        ClassTypeDescriptor jlt = ClassTypeDescriptor.synthesize(context, "java/lang/Throwable");
        int ci = typeDefinition.findConstructorIndex(MethodDescriptor.synthesize(context, BaseTypeDescriptor.V, List.of(jlt)));
        if (ci == -1) {
            throw new IllegalArgumentException("Cannot construct exception with message (no matching ctor) for " + getName());
        }
        vm.invokeExact(typeDefinition.getConstructor(ci), throwable, List.of(cause));
        return throwable;
    }

    @Override
    public VmThrowable newInstance(String message, VmThrowable cause) {
        VmThrowableImpl throwable = new VmThrowableImpl(this);
        VmImpl vm = getVm();
        vm.manuallyInitialize(throwable);
        throwable.initializeDepth();
        LoadedTypeDefinition typeDefinition = getTypeDefinition();
        ClassContext context = typeDefinition.getContext();
        ClassTypeDescriptor jls = ClassTypeDescriptor.synthesize(context, "java/lang/String");
        ClassTypeDescriptor jlt = ClassTypeDescriptor.synthesize(context, "java/lang/Throwable");
        int ci = typeDefinition.findConstructorIndex(MethodDescriptor.synthesize(context, BaseTypeDescriptor.V, List.of(jls, jlt)));
        if (ci == -1) {
            throw new IllegalArgumentException("Cannot construct exception with message (no matching ctor) for " + getName());
        }
        vm.invokeExact(typeDefinition.getConstructor(ci), throwable, List.of(vm.intern(message), cause));
        return throwable;
    }
}
