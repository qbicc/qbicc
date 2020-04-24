package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;

abstract class CloseableThread extends Thread {
    Throwable problem;

    CloseableThread(final String name) {
        super(name);
    }

    public void run() {
        try {
            runWithException();
        } catch (Throwable t) {
            problem = t;
        }
    }

    abstract void runWithException() throws IOException;
}
