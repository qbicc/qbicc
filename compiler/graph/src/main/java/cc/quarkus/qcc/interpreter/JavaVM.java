package cc.quarkus.qcc.interpreter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.type.definition.Dictionary;

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

    JavaThread currentThread();

    /**
     * Define an unresolved class into this VM.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the class loader instance ({@code null} indicates the bootstrap class loader)
     * @param bytes the class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    JavaClass defineClass(String name, JavaObject classLoader, ByteBuffer bytes);

    /**
     * Define an unresolved anonymous class into this VM.
     *
     * @param hostClass the host class (must not be {@code null})
     * @param bytes the class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    JavaClass defineAnonymousClass(JavaClass hostClass, ByteBuffer bytes);

    /**
     * Deliver a "signal" to the target environment.
     *
     * @param signal the signal to deliver
     */
    void deliverSignal(Signal signal);

    /**
     * Wait for the VM to terminate, returning the exit code.
     *
     * @return the VM exit code
     * @throws InterruptedException if the calling thread was interrupted before the VM terminates
     */
    int awaitTermination() throws InterruptedException;

    /**
     * Kill the VM, terminating all in-progress threads and releasing all heap objects.
     */
    void close();

    /**
     * Create a new virtual machine.  A list of bootstrap module JARs must be given in order to set up
     * the core system classes.
     *
     *
     * @param bootstrapLoader the bootstrap loader dictionary to use (must not be {@code null})
     * @param bootstrapModulePath the bootstrap module path (must not be {@code null})
     * @return a new virtual machine
     */
    static JavaVM create(Dictionary bootstrapLoader, List<Path> bootstrapModulePath) {
        return new JavaVMImpl(bootstrapLoader, bootstrapModulePath);
    }

}
