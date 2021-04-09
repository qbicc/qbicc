package org.qbicc.machine.tool.process;

import java.io.Closeable;
import java.io.IOException;

import org.qbicc.machine.tool.ToolExecutionFailureException;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionRunnable;

final class Closeables {
    private Closeables() {}

    static final Closeable BLANK_CLOSEABLE = new Closeable() {
        public void close() {
        }
    };

    static Closeable start(CloseableThread thread) throws IOException {
        try {
            thread.start();
        } catch (Throwable t) {
            throw new IOException("Problem starting thread", t);
        }
        return new Closeable() {
            public void close() throws IOException {
                boolean intr = false;
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
        };
    }

    static Closeable start(String name, ExceptionRunnable<IOException> body) throws IOException {
        return start(new CloseableThread(name, body));
    }

    static Closeable cleanup(final Closeable closeable, final ExceptionConsumer<IOException, IOException> action) {
        return new Closeable() {
            public void close() throws IOException {
                try {
                    closeable.close();
                    action.accept(null);
                } catch (IOException e) {
                    action.accept(e);
                }
            }
        };
    }
}
