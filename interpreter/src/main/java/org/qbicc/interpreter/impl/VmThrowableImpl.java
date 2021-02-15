package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmThrowable;

final class VmThrowableImpl extends VmObjectImpl implements VmThrowable {
    private volatile Frame stackTrace;

    VmThrowableImpl(VmClassImpl clazz) {
        super(clazz);
    }

    void fillInStackTrace(Frame frame) {
        stackTrace = frame;
    }
}
