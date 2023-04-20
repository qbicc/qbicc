package org.qbicc.interpreter.impl;

import java.util.concurrent.locks.ReentrantLock;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForThread {
    HooksForThread() {}

    @Hook
    static void yield(VmThread thread) {
        Thread.yield();
    }

    @Hook
    static void start(VmThreadImpl thread, VmThreadImpl targetThread) {
        thread.vm.startedThreads.add(targetThread);
    }

    @Hook
    static boolean holdsLock(VmThreadImpl thread, VmObjectImpl object) {
        return ((ReentrantLock)object.getLock()).isHeldByCurrentThread();
    }

    @Hook
    static boolean isAlive(VmThreadImpl thread, VmThreadImpl targetThread) {
        final Thread boundThread = targetThread.boundThread;
        return boundThread != null && boundThread.isAlive();
    }

    @Hook
    static VmThreadImpl currentThread(VmThreadImpl thread) {
        return thread;
    }

    @Hook
    static void onSpinWait(VmThreadImpl thread) {
        Thread.onSpinWait();
    }

    @Hook(descriptor = "(J)V")
    static void sleep(VmThreadImpl thread, long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new Thrown(thread.vm.interruptedException.newInstance());
        }
    }

    @Hook(descriptor = "(JI)V")
    static void sleep(VmThreadImpl thread, long millis, int nanos) {
        try {
            Thread.sleep(millis, nanos);
        } catch (InterruptedException e) {
            throw new Thrown(thread.vm.interruptedException.newInstance());
        }
    }

    @Hook
    static boolean isInterrupted(VmThreadImpl thread, VmThreadImpl targetThread) {
        return targetThread.boundThread.isInterrupted();
    }

    @Hook
    static boolean interrupted(VmThreadImpl thread) {
        assert thread.boundThread == Thread.currentThread();
        return Thread.interrupted();
    }

    @Hook
    static void interrupt(VmThreadImpl thread, VmThreadImpl targetThread) {
        targetThread.boundThread.interrupt();
    }
}
