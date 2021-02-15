package org.qbicc.interpreter;

import java.nio.ByteBuffer;
import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;
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
        return requireCurrentThread().getVM();
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
     * @param classContext the class context instance (must not be {@code null})
     * @param name the internal name of the class to load (must not be {@code null})
     * @return the class (not {@code null})
     * @throws Thrown if the internal JVM has thrown an exception while loading the class
     */
    DefinedTypeDefinition loadClass(ClassContext classContext, String name) throws Thrown;

    /**
     * Allocate an object without initializing it (all fields/elements will be {@code null}, {@code false}, or zero).
     * If the given {@code type} is not initialized, it will be.
     *
     * @param type the type to allocate (must not be {@code null})
     * @return the allocated object (not {@code null})
     */
    VmObject allocateObject(ClassObjectType type);

    /**
     * Invoke a constructor reflectively.  Primitive arguments should be boxed.
     *
     * @param method the constructor to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the constructor's expectations
     */
    void invokeExact(ConstructorElement method, VmObject instance, List<Object> args);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeExact(MethodElement method, VmObject instance, List<Object> args);

    void initialize(VmClass vmClass);

    /**
     * Invoke a method reflectively.  Primitive arguments should be boxed.
     *
     * @param method the method to invoke
     * @param instance the instance to invoke upon
     * @param args the arguments, whose times must match the method's expectations
     * @return the result
     */
    Object invokeVirtual(MethodElement method, VmObject instance, List<Object> args);

    /**
     * Deliver a "signal" to the target environment.
     *
     * @param signal the signal to deliver
     */
    void deliverSignal(Signal signal);

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
    DefinedTypeDefinition.Builder newTypeDefinitionBuilder(VmClassLoader classLoader);

    /**
     * Get the main (root) thread group for the VM.
     *
     * @return the thread group (not {@code null})
     */
    VmObject getMainThreadGroup();

    /**
     * Allocate memory of the given size.  If the memory size is 0, a shared instance may be returned.
     *
     * @param size the memory size (must be 0 or greater)
     * @return the memory (not {@code null})
     */
    Memory allocate(int size);

    /**
     * Convenience method to get the actual (non-{@code null}) class loader for the given class context.
     *
     * @param classContext the class context (must not be {@code null})
     * @return the class loader (not {@code null}, may be the bootstrap class loader)
     */
    VmClassLoader getClassLoaderForContext(ClassContext classContext);
}
