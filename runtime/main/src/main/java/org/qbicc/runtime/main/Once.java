package org.qbicc.runtime.main;

import java.util.Objects;

/**
 * A one-time operation.
 */
public final class Once {

    private final Object lock = new Object();
    private Runnable target;
    private Throwable thrown;
    private volatile boolean done;

    /**
     * Construct a new instance.
     *
     * @param target the target to run
     */
    public Once(Runnable target) {
        Objects.requireNonNull(target, "target");
        this.target = target;
        // publish
        done = false;
    }

    /**
     * Execute the one-time task.
     * If it throws an exception, the thrown object propagates to the caller.
     * If the task was previously run successfully, this method does nothing.
     * If the task was previously run but failed, the same thrown object is propagated to the caller.
     * If the task is in the process of being run by the current thread, this method returns without doing anything.
     * If the task is in the process of being run by some other thread, the calling thread will block until the task completes.
     *
     * @throws Throwable the object thrown by the target task
     */
    @SuppressWarnings("unused") // invoked by compiler-generated code
    public void run() throws Throwable {
        boolean done = this.done;
        if (! done) {
            if (Thread.holdsLock(lock)) {
                // we're already running it
                return;
            }
            synchronized (lock) {
                done = this.done;
                if (! done) {
                    Throwable thrown = this.thrown;
                    if (thrown != null) {
                        throw thrown;
                    }
                    try {
                        target.run();
                    } catch (Throwable t) {
                        this.thrown = t;
                        target = null;
                        throw t;
                    }
                    target = null;
                    this.done = true;
                    return;
                }
            }
        }
    }
}
