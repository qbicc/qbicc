package cc.quarkus.qcc.interpreter;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

/**
 * A virtual machine.
 */
public interface Vm extends AutoCloseable {
    /**
     * Create a new thread.
     *
     * @param threadName the thread name
     * @param threadGroup the thread group object (or {@code null})
     * @param daemon {@code true} to make a daemon thread
     * @return the new thread
     */
    VmThread newThread(String threadName, VmObject threadGroup, boolean daemon);

    static VmThread currentThread() {
        return VmImpl.currentThread();
    }

    static VmThread requireCurrentThread() {
        VmThread vmThread = currentThread();
        if (vmThread == null) {
            throw new IllegalStateException("Thread is not attached");
        }
        return vmThread;
    }

    static Vm current() {
        return VmImpl.currentVm();
    }

    static Vm requireCurrent() {
        Vm current = current();
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
    DefinedTypeDefinition defineClass(String name, VmObject classLoader, ByteBuffer bytes);

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
    DefinedTypeDefinition loadClass(VmObject classLoader, String name) throws Thrown;

    /**
     * Find a loaded class, returning {@code null} if the class loader did not previously load the class.  The VM
     * is not entered.
     *
     * @param classLoader the class loader instance ({@code null} indicates the bootstrap class loader)
     * @param name the internal name of the class to load (must not be {@code null})
     * @return the class, or {@code null} if the class was not already loaded
     */
    DefinedTypeDefinition findLoadedClass(VmObject classLoader, String name);

    /**
     * Allocate an object without initializing it (all fields/elements will be {@code null}, {@code false}, or zero).
     * If the given {@code type} is not initialized, it will be.
     *
     * @param type the type to allocate (must not be {@code null})
     * @return the allocated object (not {@code null})
     */
    VmObject allocateObject(ClassTypeIdLiteral type);

    VmArray allocateArray(ArrayTypeIdLiteral type, int length);

    /**
     * Invoke a constructor reflectively.  Primitive arguments should be boxed.
     *
     * @param method the constructor to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the constructor's expectations
     */
    void invokeExact(ConstructorElement method, VmObject instance, Object... args);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeExact(MethodElement method, VmObject instance, Object... args);

    void initialize(TypeIdLiteral typeId);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeVirtual(MethodElement method, final VmObject instance, Object... args);

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
    String deduplicate(VmObject classLoader, String string);

    String deduplicate(VmObject classLoader, ByteBuffer buffer, int offset, int length, boolean expectTerminator);

    /**
     * Get a shared string instance.  The same string object will be reused for a given input string.
     *
     * @param string the input string (must not be {@code null})
     * @return the instance
     */
    VmObject getSharedString(String string);

    /**
     * Allocate a direct byte buffer object with the given backing buffer.  The backing content will be determined
     * by the limit and position of the given buffer at the time this method is called.  The given buffer may be
     * direct or heap-based.
     *
     * @param backingBuffer the backing buffer (must not be {@code null})
     * @return the allocated direct buffer object (not {@code null})
     */
    VmObject allocateDirectBuffer(ByteBuffer backingBuffer);

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
    DefinedTypeDefinition.Builder newTypeDefinitionBuilder(VmObject classLoader);

    /**
     * Get a builder for a new VM.
     *
     * @return the builder (not {@code null})
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Get the main (root) thread group for the VM.
     *
     * @return the thread group (not {@code null})
     */
    VmObject getMainThreadGroup();

    /**
     * A builder for the VM.
     */
    class Builder {
        final List<Path> bootstrapModules = new ArrayList<>();
        final List<Path> platformModules = new ArrayList<>();
        final Map<String, String> systemProperties = new HashMap<>();
        CompilationContext context;

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
            systemProperties.put(Assert.checkNotNullParam("propertyName", propertyName), Assert.checkNotNullParam("propertyValue", propertyValue));
            return this;
        }

        public Builder setContext(final CompilationContext context) {
            this.context = Assert.checkNotNullParam("context", context);
            return this;
        }

        /**
         * Construct the new VM.
         *
         * @return the new VM (not {@code null})
         */
        public Vm build() {
            return new VmImpl(this);
        }
    }
}
