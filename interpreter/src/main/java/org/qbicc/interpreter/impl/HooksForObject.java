package org.qbicc.interpreter.impl;

import java.util.concurrent.TimeUnit;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmThrowable;

/**
 * Interpreter hooks for {@code java.lang.Object}.
 */
final class HooksForObject {
    HooksForObject() {}

    @Hook(name = "clone")
    static Object cloneImpl(VmThreadImpl thread, VmObjectImpl target) {
        final VmImpl vm = thread.getVM();
        final VmClassLoaderImpl bootstrapClassLoader = vm.bootstrapClassLoader;
        VmClassImpl cloneableClass = bootstrapClassLoader.loadClass("java/lang/Cloneable");
        if (!target.getVmClass().getTypeDefinition().isSubtypeOf(cloneableClass.getTypeDefinition())) {
            VmClassImpl cnse = bootstrapClassLoader.loadClass("java/lang/CloneNotSupportedException");
            VmThrowable throwable = vm.manuallyInitialize((VmThrowable) cnse.newInstance());
            throw new Thrown(throwable);
        }
        return target.clone();
    }

    @Hook(name = "wait", descriptor = "()V")
    static void waitImpl(VmThreadImpl thread, VmObjectImpl target) {
        final VmImpl vm = thread.getVM();
        try {
            target.getCondition().await();
        } catch (IllegalMonitorStateException e) {
            throw new Thrown(vm.illegalMonitorStateException.newInstance());
        } catch (InterruptedException e) {
            throw new Thrown(vm.interruptedException.newInstance());
        }
    }

    @Hook(name = "wait", descriptor = "(J)V")
    static void waitImpl(VmThreadImpl thread, VmObjectImpl target, long millis) {
        final VmImpl vm = thread.getVM();
        try {
            target.getCondition().await(millis, TimeUnit.MILLISECONDS);
        } catch (IllegalMonitorStateException e) {
            throw new Thrown(vm.illegalMonitorStateException.newInstance());
        } catch (InterruptedException e) {
            throw new Thrown(vm.interruptedException.newInstance());
        }
    }

    @Hook(name = "wait", descriptor = "(JI)V")
    static void waitImpl(VmThreadImpl thread, VmObjectImpl target, long millis, int nanos) {
        final VmImpl vm = thread.getVM();
        try {
            if (nanos > 0 && millis < Long.MAX_VALUE) {
                millis++;
            }
            target.getCondition().await(millis, TimeUnit.MILLISECONDS);
        } catch (IllegalMonitorStateException e) {
            throw new Thrown(vm.illegalMonitorStateException.newInstance());
        } catch (InterruptedException e) {
            throw new Thrown(vm.interruptedException.newInstance());
        }
    }

    @Hook(name = "notify")
    static void notifyImpl(VmThreadImpl thread, VmObjectImpl target) {
        try {
            target.getCondition().signal();
        } catch (IllegalMonitorStateException e) {
            throw new Thrown(thread.getVM().illegalMonitorStateException.newInstance());
        }
    }

    @Hook(name = "notifyAll")
    static void notifyAllImpl(VmThreadImpl thread, VmObjectImpl target) {
        try {
            target.getCondition().signalAll();
        } catch (IllegalMonitorStateException e) {
            throw new Thrown(thread.getVM().illegalMonitorStateException.newInstance());
        }
    }
}
