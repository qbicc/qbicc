package cc.quarkus.qcc.interpreter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cc.quarkus.qcc.graph.Invocation;

final class VmThreadImpl implements VmThread {
    final VmImpl vm;
    final VmObjectImpl instance;
    State state = State.RUNNING;
    Thread attachedThread;
    VmImpl.StackFrame tos;
    Lock threadLock = new ReentrantLock();

    VmThreadImpl(final String threadName, final VmObject threadGroup, final boolean daemon, final VmImpl vm) {
        this.vm = vm;
        instance = new VmObjectImpl(vm.threadClass.validate());
        // todo: initialize thread fields...
    }

    public void doAttached(final Runnable r) {
        vm.doAttached(() -> {
            VmThreadImpl currentlyAttached = vm.attachedThread.get();
            if (currentlyAttached == this) {
                r.run();
                return;
            }
            if (currentlyAttached != null) {
                throw new IllegalStateException("Another thread is already attached");
            }
            threadLock.lock();
            try {
                if (state != State.RUNNING) {
                    throw new IllegalStateException("Thread is not running");
                }
                if (attachedThread != null) {
                    throw new IllegalStateException("Thread is already attached");
                }
                attachedThread = Thread.currentThread();
            } finally {
                threadLock.unlock();
            }
            vm.attachedThread.set(this);
            try {
                r.run();
            } finally {
                vm.attachedThread.remove();
                threadLock.lock();
                attachedThread = null;
                threadLock.unlock();
            }
        });
    }

    VmImpl.StackFrame pushNewFrame(Invocation caller) {
        return tos = new VmImpl.StackFrame(tos, caller);
    }

    VmImpl.StackFrame popFrame() {
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

    public Vm getVM() {
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
