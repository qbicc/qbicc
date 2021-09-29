package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.interpreter.VmThrowableClass;
import org.qbicc.type.definition.LoadedTypeDefinition;

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
        throwable.initializeDepth();
        // todo: call ctor
        return throwable;
    }

    @Override
    public VmThrowable newInstance(String message, VmThrowable cause) {
        VmThrowableImpl throwable = new VmThrowableImpl(this);
        throwable.initializeDepth();
        // todo: call ctor
        return throwable;
    }
}
