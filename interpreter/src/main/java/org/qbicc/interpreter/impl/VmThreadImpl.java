package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

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

    void setThreadGroup(final VmObject threadGroup) {
        int offset = indexOf(clazz.getTypeDefinition().findField("group"));
        memory.storeRef(offset, threadGroup, SinglePlain);
    }

    void setPriority(final int priority) {
        int offset = indexOf(clazz.getTypeDefinition().findField("priority"));
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException("Invalid thread priority: "+priority);
        }
        memory.store32(offset, priority, SinglePlain);
    }

    VmClassLoader getContextClassLoader() {
        int offset = indexOf(clazz.getTypeDefinition().findField("contextClassLoader"));
        return (VmClassLoader) memory.loadRef(offset, SinglePlain);
    }

    void setContextClassLoader(VmClassLoader cl) {
        int offset = indexOf(clazz.getTypeDefinition().findField("contextClassLoader"));
        memory.storeRef(offset, cl, SinglePlain);
    }

    void setBoundThread(Thread boundThread) {
        this.boundThread = boundThread;
    }

    Thread getBoundThread() {
        return boundThread;
    }
}
