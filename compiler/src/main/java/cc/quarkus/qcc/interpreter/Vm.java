package cc.quarkus.qcc.interpreter;

import java.nio.ByteBuffer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;

/**
 * A virtual machine.
 */
public interface Vm {

    /**
     * Get the related compilation context.
     *
     * @return the related compilation context
     */
    CompilationContext getCompilationContext();

    /**
     * Create a new thread.
     *
     * @param threadName the thread name
     * @param threadGroup the thread group object (or {@code null})
     * @param daemon {@code true} to make a daemon thread
     * @return the new thread
     */
    VmThread newThread(String threadName, VmObject threadGroup, boolean daemon);

    /**
     * Perform the given action with the given thread attached to the host thread.  Any previously attached thread is
     * suspended.
     *
     * @param thread the thread to attach (must not be {@code null})
     * @param action the action to perform (must not be {@code null})
     */
    default void doAttached(VmThread thread, Runnable action) {
        Assert.checkNotNullParam("thread", thread);
        Assert.checkNotNullParam("action", action);
        VmThread old = VmPrivate.CURRENT_THREAD.get();
        VmPrivate.CURRENT_THREAD.set(thread);
        try {
            action.run();
        } finally {
            if (old == null) {
                VmPrivate.CURRENT_THREAD.remove();
            } else {
                VmPrivate.CURRENT_THREAD.set(old);
            }
        }
    }

    static VmThread currentThread() {
        return VmPrivate.CURRENT_THREAD.get();
    }

    static VmThread requireCurrentThread() {
        VmThread vmThread = currentThread();
        if (vmThread == null) {
            throw new IllegalStateException("Thread is not attached");
        }
        return vmThread;
    }

    static Vm current() {
        return currentThread().getVM();
    }

    static Vm requireCurrent() {
        Vm current = current();
        if (current == null) {
            throw new IllegalStateException("JavaVM is not attached");
        }
        return current;
    }

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
    VmObject allocateObject(ClassObjectType type);

    VmArray allocateArray(ArrayObjectType type, int length);

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

    void initialize(VmClass vmClass);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeVirtual(MethodElement method, VmObject instance, Object... args);

    /**
     * Deliver a "signal" to the target environment.
     *
     * @param signal the signal to deliver
     */
    void deliverSignal(Signal signal);

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
     * Get a builder for a type definition.  The given class loader is only used to resolve classes, and does
     * not cause the new class to be registered to that class loader (which is something that must be done
     * by the dictionary).
     *
     * @param classLoader the class loader for the new class, or {@code null} for the bootstrap class loader
     * @return the builder
     */
    DefinedTypeDefinition.Builder newTypeDefinitionBuilder(VmObject classLoader);

    /**
     * Get the main (root) thread group for the VM.
     *
     * @return the thread group (not {@code null})
     */
    VmObject getMainThreadGroup();
}
