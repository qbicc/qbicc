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

    JavaThread currentThread() throws IllegalStateException;

    /**
     * Register a host method to be executed when a native method is invoked.
     *
     * @param javaClass the declaring class of the method
     * @param methodName the method name
     * @param methodSignature the method signature string
     * @param method the method to invoke or {@code null} to clear the registration
     * @return the previously-registered host method (if any)
     */
    HostMethod registerNativeMethod(JavaClass javaClass, String methodName, String methodSignature, HostMethod method);

    /**
     * Kill the VM, terminating all in-progress threads and releasing all heap objects.
     */
    void close();
}
