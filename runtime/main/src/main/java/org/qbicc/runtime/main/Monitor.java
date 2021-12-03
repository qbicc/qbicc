package org.qbicc.runtime.main;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.qbicc.runtime.Hidden;

/**
 * An object monitor (lock) implementation. This is the platform-independent implementation and should not be assumed to be
 * the actual monitor implementation for some targets. This implementation is guaranteed to be based on
 * {@link java.util.concurrent.locks.LockSupport LockSupport}'s {@code park} mechanism.
 */
@SuppressWarnings("unused")
public final class Monitor {
    private static final long MAX_MILLIS = Long.MAX_VALUE / 1_000_000L;

    private final ReentrantLock lock;
    private final Condition condition;

    /**
     * Construct a new instance.
     */
    public Monitor() {
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    @Hidden
    public boolean isHeldByCurrentThread() {
        return lock.isHeldByCurrentThread();
    }

    @Hidden
    public void enter() {
        lock.lock();
    }

    @Hidden
    public void exit() throws IllegalMonitorStateException {
        lock.unlock();
    }

    @Hidden
    public void await() throws InterruptedException {
        condition.await();
    }

    @Hidden
    public void await(long millis) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("Invalid milliseconds");
        }
        condition.await(millis, TimeUnit.MILLISECONDS);
    }

    @Hidden
    public void await(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("Invalid milliseconds");
        } else if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("Invalid nanoseconds");
        } else if (millis < MAX_MILLIS) {
            // be exact
            condition.await(millis * 1_000_000L + nanos, TimeUnit.NANOSECONDS);
        } else {
            if (nanos > 0) {
                millis++;
            }
            condition.await(millis, TimeUnit.MILLISECONDS);
        }
    }

    @Hidden
    public void signal() throws IllegalMonitorStateException {
        condition.signal();
    }

    @Hidden
    public void signalAll() throws IllegalMonitorStateException {
        condition.signalAll();
    }
}
