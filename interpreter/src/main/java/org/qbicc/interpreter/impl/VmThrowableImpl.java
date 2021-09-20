package org.qbicc.interpreter.impl;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.VmThrowable;

final class VmThrowableImpl extends VmObjectImpl implements VmThrowable {
    private volatile Frame stackTrace;

    VmThrowableImpl(VmClassImpl clazz) {
        super(clazz);
    }

    void fillInStackTrace(Frame frame) {
        stackTrace = frame;
    }

    public String getMessage() {
        VmClassImpl vmClass = getVmClass().getVm().throwableClass;
        int offs = vmClass.getLayoutInfo().getMember(vmClass.getTypeDefinition().findField("detailMessage")).getOffset();
        VmStringImpl messageStr = (VmStringImpl) getMemory().loadRef(offs, MemoryAtomicityMode.UNORDERED);
        return messageStr == null ? null : messageStr.getContent();
    }
}
