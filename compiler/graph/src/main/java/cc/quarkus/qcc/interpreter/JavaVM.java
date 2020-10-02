package cc.quarkus.qcc.interpreter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.graph.ArrayClassType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.GraphFactory;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

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

    static JavaThread currentThread() {
        return JavaVMImpl.currentThread();
    }

    static JavaThread requireCurrentThread() {
        JavaThread javaThread = currentThread();
        if (javaThread == null) {
            throw new IllegalStateException("Thread is not attached");
        }
        return javaThread;
    }

    static JavaVM current() {
        return JavaVMImpl.currentVm();
    }

    static JavaVM requireCurrent() {
        JavaVM current = current();
        if (current == null) {
            throw new IllegalStateException("JavaVM is not attached");
        }
        return current;
    }

    void doAttached(Runnable r);

    DefinedTypeDefinition getClassTypeDefinition();

    DefinedTypeDefinition getObjectTypeDefinition();

    /**
     * Define an unresolved class into this VM.
     *
     * @param name the class name (must not be {@code null})
     * @param classLoader the class loader instance ({@code null} indicates the bootstrap class loader)
     * @param bytes the class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    DefinedTypeDefinition defineClass(String name, JavaObject classLoader, ByteBuffer bytes);

    /**
     * Define an unresolved anonymous class into this VM.
     *
     * @param hostClass the host class (must not be {@code null})
     * @param bytes the class bytes (must not be {@code null})
     * @return the defined class (not {@code null})
     */
    DefinedTypeDefinition defineAnonymousClass(DefinedTypeDefinition hostClass, ByteBuffer bytes);

    /**
     * Load a class. If the class is already loaded, it is returned without entering the VM.  Otherwise the
     * current thread must be bound to a VM thread.
     *
     * @param classLoader the class loader instance ({@code null} indicates the bootstrap class loader)
     * @param name the internal name of the class to load (must not be {@code null})
     * @return the class (not {@code null})
     * @throws Thrown if the internal JVM has thrown an exception while loading the class
     */
    DefinedTypeDefinition loadClass(JavaObject classLoader, String name) throws Thrown;

    /**
     * Find a loaded class, returning {@code null} if the class loader did not previously load the class.  The VM
     * is not entered.
     *
     * @param classLoader the class loader instance ({@code null} indicates the bootstrap class loader)
     * @param name the internal name of the class to load (must not be {@code null})
     * @return the class, or {@code null} if the class was not already loaded
     */
    DefinedTypeDefinition findLoadedClass(JavaObject classLoader, String name);

    /**
     * Allocate an object without initializing it (all fields/elements will be {@code null}, {@code false}, or zero).
     * If the given {@code type} is not initialized, it will be.
     *
     * @param type the type to allocate (must not be {@code null})
     * @return the allocated object (not {@code null})
     */
    JavaObject allocateObject(ClassType type);

    JavaArray allocateArray(ArrayClassType type, int length);

    /**
     * Invoke a constructor reflectively.  Primitive arguments should be boxed.
     *
     * @param method the constructor to invoke
     * @param args the arguments, whose times must match the constructor's expectations
     */
    void invokeExact(ConstructorElement method, Object... args);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeExact(MethodElement method, Object... args);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeVirtual(MethodElement method, Object... args);

    Object invoke(MethodHandle handle, Object... args);

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
     * Get a deduplicated string.
     *
     * @param classLoader the class loader whose deduplication table should be used
     * @param string the string to deduplicate
     * @return the deduplicated string
     */
    String deduplicate(JavaObject classLoader, String string);

    String deduplicate(JavaObject classLoader, ByteBuffer buffer, int offset, int length, boolean expectTerminator);

    /**
     * Allocate a direct byte buffer object with the given backing buffer.  The backing content will be determined
     * by the limit and position of the given buffer at the time this method is called.  The given buffer may be
     * direct or heap-based.
     *
     * @param backingBuffer the backing buffer (must not be {@code null})
     * @return the allocated direct buffer object (not {@code null})
     */
    JavaObject allocateDirectBuffer(ByteBuffer backingBuffer);

    /**
     * Kill the VM, terminating all in-progress threads and releasing all heap objects.
     */
    void close();

    /**
     * Get a builder for a type definition.  The given class loader is only used to resolve classes, and does
     * not cause the new class to be registered to that class loader (which is something that must be done
     * by the dictionary).
     *
     * @param classLoader the class loader for the new class, or {@code null} for the bootstrap class loader
     * @return the builder
     */
    DefinedTypeDefinition.Builder newTypeDefinitionBuilder(JavaObject classLoader);

    /**
     * Get a builder for a new VM.
     *
     * @return the builder (not {@code null})
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new graph factory.
     *
     * @return the graph factory
     */
    GraphFactory createGraphFactory();

    /**
     * Get the main (root) thread group for the VM.
     *
     * @return the thread group (not {@code null})
     */
    JavaObject getMainThreadGroup();

    /**
     * A builder for the VM.
     */
    class Builder {
        final List<Path> bootstrapModules = new ArrayList<>();
        final List<Path> platformModules = new ArrayList<>();
        GraphFactory graphFactory = GraphFactory.BASIC_FACTORY;
        final Map<String, String> systemProperties = new HashMap<>();

        Builder() {
        }

        /**
         * Add a bootstrap module.
         *
         * @param modulePath the path to the module JAR (must not be {@code null})
         * @return this builder
         */
        public Builder addBootstrapModule(Path modulePath) {
            bootstrapModules.add(Assert.checkNotNullParam("modulePath", modulePath));
            return this;
        }

        /**
         * Add all of the given bootstrap modules.
         *
         * @param modulePaths the paths to the module JARs (must not be {@code null})
         * @return this builder
         */
        public Builder addBootstrapModules(final List<Path> modulePaths) {
            bootstrapModules.addAll(modulePaths);
            return this;
        }

        /**
         * Add a platform (non-bootstrap) module.
         *
         * @param modulePath the path to the module JAR (must not be {@code null})
         * @return this builder
         */
        public Builder addPlatformModule(Path modulePath) {
            platformModules.add(Assert.checkNotNullParam("modulePath", modulePath));
            return this;
        }

        /**
         * Set an initial system property.
         *
         * @param propertyName  the property name (must not be {@code null})
         * @param propertyValue the property value (must not be {@code null})
         * @return this builder
         */
        public Builder setSystemProperty(String propertyName, String propertyValue) {
            systemProperties.put(Assert.checkNotNullParam("propertyName", propertyName), Assert.checkNotNullParam("propertyValue)", propertyValue));
            return this;
        }

        /**
         * Set the graph factory to use for bytecode parsing.
         *
         * @param graphFactory the graph factory to use (must not be {@code null})
         * @return this builder
         */
        public Builder setGraphFactory(final GraphFactory graphFactory) {
            this.graphFactory = Assert.checkNotNullParam("graphFactory", graphFactory);
            return this;
        }

        /**
         * Construct the new VM.
         *
         * @return the new VM (not {@code null})
         */
        public JavaVM build() {
            return new JavaVMImpl(this);
        }
    }
}
