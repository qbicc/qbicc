package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.memory.MemoryFactory;
import org.qbicc.pointer.MemoryPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.ClassObjectType;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

final class VmThreadImpl extends VmObjectImpl implements VmThread {
    private final Pointer selfPointer;
    final VmImpl vm;
    volatile Thread boundThread;
    Frame currentFrame;

    VmThreadImpl(VmClassImpl clazz, VmImpl vm) {
        super(clazz);
        this.vm = vm;
        ClassObjectType threadType = clazz.getTypeDefinition().getClassType();
        selfPointer = new MemoryPointer(threadType.getPointer(), MemoryFactory.wrap(new VmObject[] { this }, threadType.getReference()));
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

    VmClassLoader getContextClassLoader() {
        int offset = indexOf(clazz.getTypeDefinition().findField("contextClassLoader"));
        return (VmClassLoader) memory.loadRef(offset, SinglePlain);
    }

    void setContextClassLoader(VmClassLoader cl) {
        int offset = indexOf(clazz.getTypeDefinition().findField("contextClassLoader"));
        memory.storeRef(offset, cl, SinglePlain);
    }

    void setBoundThread(Thread boundThread) {
        if (this.boundThread != null && this.boundThread != boundThread) {
            throw new IllegalStateException("Cannot rebind thread");
        }
        this.boundThread = boundThread;
    }

    Thread getBoundThread() {
        return boundThread;
    }

    Pointer getSelfPointer() {
        return selfPointer;
    }
}
