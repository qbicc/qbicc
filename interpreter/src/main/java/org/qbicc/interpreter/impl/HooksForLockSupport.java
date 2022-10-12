package org.qbicc.interpreter.impl;

import java.util.concurrent.locks.LockSupport;

import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;

/**
 *
 */
final class HooksForLockSupport {
    HooksForLockSupport() {}

    @Hook(descriptor = "()V")
    static void park(VmThread thread) {
        LockSupport.park();
    }

    @Hook(descriptor = "(Ljava/lang/Object;)V")
    static void park(VmThread thread, VmObject blocker) {
        LockSupport.park(blocker);
    }

    @Hook(descriptor = "(J)V")
    static void parkNanos(VmThread thread, long nanos) {
        LockSupport.parkNanos(nanos);
    }

    @Hook(descriptor = "(Ljava/lang/Object;J)V")
    static void parkNanos(VmThread thread, VmObject blocker, long nanos) {
        LockSupport.parkNanos(blocker, nanos);
    }

    @Hook(descriptor = "(J)V")
    static void parkUntil(VmThread thread, long deadline) {
        LockSupport.parkUntil(deadline);
    }

    @Hook(descriptor = "(Ljava/lang/Object;J)V")
    static void parkUntil(VmThread thread, VmObject blocker, long deadline) {
        LockSupport.parkUntil(blocker, deadline);
    }

    @Hook
    static void setCurrentBlocker(VmThread thread, VmObject blocker) {
        LockSupport.setCurrentBlocker(blocker);
    }

    @Hook
    static void unpark(VmThread thread, VmThreadImpl targetThread) {
        LockSupport.unpark(targetThread.getBoundThread());
    }
}
