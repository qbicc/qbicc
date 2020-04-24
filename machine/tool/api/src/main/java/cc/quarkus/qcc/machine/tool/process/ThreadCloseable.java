package cc.quarkus.qcc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;

import cc.quarkus.qcc.machine.tool.ToolExecutionFailureException;

/**
 *
 */
final class ThreadCloseable implements Closeable {
    private final CloseableThread thread;

    ThreadCloseable(final CloseableThread thread) throws IOException {
        this.thread = thread;
        try {
            thread.start();
        } catch (Throwable t) {
            throw new IOException("Problem starting thread", t);
        }
    }

    public void close() throws IOException {
        boolean intr = Thread.interrupted();
        try {
            for (;;)
                try {
                    thread.join();
                    final Throwable problem = thread.problem;
                    if (problem != null) {
                        try {
                            throw problem;
                        } catch (ToolExecutionFailureException | RuntimeException | Error e) {
                            throw e;
                        } catch (Throwable t) {
                            throw new ToolExecutionFailureException("Tool output collection failed", t);
                        }
                    }
                    return;
                } catch (InterruptedException ignored) {
                    thread.interrupt();
                    intr = true;
                }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }


}
