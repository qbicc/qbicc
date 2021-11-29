package org.qbicc.interpreter.impl;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.type.definition.element.FieldElement;

final class VmThreadImpl extends VmObjectImpl implements VmThread {
    final VmImpl vm;
    volatile Thread boundThread;
    Frame currentFrame;

    VmThreadImpl(VmClassImpl clazz, VmImpl vm) {
        super(clazz);
        this.vm = vm;
    }

    @Override
    public VmImpl getVM() {
        return vm;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void await() {

    }

    void setThrown(final VmThrowable throwable) {
        FieldElement thrownField = vm.getCompilationContext().getExceptionField();
        int offset = getVmClass().getLayoutInfo().getMember(thrownField).getOffset();
        getMemory().storeRef(offset, throwable, MemoryAtomicityMode.NONE);
    }

    void setThreadGroup(final VmObject threadGroup) {
        int offset = indexOf(clazz.getTypeDefinition().findField("group"));
        memory.storeRef(offset, threadGroup, MemoryAtomicityMode.UNORDERED);
    }

    void setPriority(final int priority) {
        int offset = indexOf(clazz.getTypeDefinition().findField("priority"));
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException("Invalid thread priority: "+priority);
        }
        memory.store32(offset, priority, MemoryAtomicityMode.UNORDERED);
    }

    void setBoundThread(Thread boundThread) {
        this.boundThread = boundThread;
    }

    Thread getBoundThread() {
        return boundThread;
    }
}
