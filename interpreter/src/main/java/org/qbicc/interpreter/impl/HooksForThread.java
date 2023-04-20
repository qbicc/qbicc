package org.qbicc.interpreter.impl;

import java.util.concurrent.locks.ReentrantLock;

import org.qbicc.interpreter.Hook;
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
}
