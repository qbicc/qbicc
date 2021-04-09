package org.qbicc.machine.tool.process;

import java.io.IOException;

import io.smallrye.common.function.ExceptionRunnable;

final class CloseableThread extends Thread {
    private final ExceptionRunnable<IOException> runnable;

    Throwable problem;

    CloseableThread(final String name) {
        this(name, () -> {});
    }

    CloseableThread(final String name, final ExceptionRunnable<IOException> runnable) {
        super(name);
        this.runnable = runnable;
    }

    public final void run() {
        try {
            runWithException();
        } catch (Throwable t) {
            problem = t;
        }
    }

    void runWithException() throws IOException {
        runnable.run();
    }
}
