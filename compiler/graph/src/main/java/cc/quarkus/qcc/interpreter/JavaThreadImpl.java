package cc.quarkus.qcc.interpreter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cc.quarkus.qcc.graph.Invocation;

final class JavaThreadImpl implements JavaThread {
    final JavaVMImpl vm;
    final JavaObjectImpl instance;
    State state = State.RUNNING;
    Thread attachedThread;
    JavaVMImpl.StackFrame tos;
    Lock threadLock = new ReentrantLock();

    JavaThreadImpl(final String threadName, final JavaObject threadGroup, final boolean daemon, final JavaVMImpl vm) {
        this.vm = vm;
        instance = new JavaObjectImpl(vm.threadClass.verify());
        // todo: initialize thread fields...
    }

    public void doAttached(final Runnable r) {
        boolean detach;
        threadLock.lock();
        try {
            if (state != State.RUNNING) {
                throw new IllegalStateException("Thread is not running");
            }
            if (attachedThread != null) {
                throw new IllegalStateException("Thread is already attached");
            }
            detach = vm.tryAttach(this);
            attachedThread = Thread.currentThread();
        } finally {
            threadLock.unlock();
        }
        try {
            r.run();
        } finally {
            threadLock.lock();
            if (detach) {
                vm.detach(this);
            }
            assert attachedThread == Thread.currentThread();
            attachedThread = null;
            threadLock.unlock();
        }
    }

    JavaVMImpl.StackFrame pushNewFrame(Invocation caller) {
        return tos = new JavaVMImpl.StackFrame(tos, caller);
    }

    JavaVMImpl.StackFrame popFrame() {
        try {
            return tos;
        } finally {
            tos = tos.getParent();
        }
    }

    void checkThread() {
        threadLock.lock();
        try {
            if (attachedThread != Thread.currentThread()) {
                throw new IllegalStateException("Thread is not attached");
            }
        } finally {
            threadLock.unlock();
        }
    }

    public JavaVM getVM() {
        return vm;
    }

    public boolean isRunning() {
        threadLock.lock();
        try {
            return state == State.RUNNING;
        } finally {
            threadLock.unlock();
        }
    }

    public boolean isFinished() {
        threadLock.lock();
        try {
            return state == State.FINISHED;
        } finally {
            threadLock.unlock();
        }
    }

    public void await() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        threadLock.lock();
        try {
            throw new UnsupportedOperationException();
        } finally {
            threadLock.unlock();
        }
    }

    enum State {
        RUNNING,
        FINISHED,
        ;
    }
}
