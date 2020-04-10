package cc.quarkus.vm.api;

/**
 * A virtual machine.
 */
public interface JavaVM extends AutoCloseable {
    /**
     * Create a new thread.
     *
     * @param threadName the thread name
     * @param threadGroup the thread group object (or {@code null})
     * @param daemon {@code true} to make a daemon thread
     * @return the new thread
     */
    JavaThread newThread(String threadName, JavaObject threadGroup, boolean daemon);

    /**
     * Kill the VM, terminating all in-progress threads and releasing all heap objects.
     */
    void close();
}
