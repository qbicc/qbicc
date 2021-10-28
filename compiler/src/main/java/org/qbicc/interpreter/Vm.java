package org.qbicc.interpreter;

import java.nio.ByteBuffer;
import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.Primitive;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.methodhandle.MethodHandleConstant;

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
     * Perform VM initialization in the currently attached thread.
     */
    void initialize();

    /**
     * Create a new thread.
     *
     * @param threadName the thread name
     * @param threadGroup the thread group object (or {@code null})
     * @param daemon {@code true} to make a daemon thread
     * @return the new thread
     */
    VmThread newThread(String threadName, VmObject threadGroup, boolean daemon, int priority);

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
     * Intern a host JVM String as a VmString
     * @param string the host string to intern
     * @return the interned VmString
     */
    VmString intern(String string);

    /**
     * Convenience method to get the actual (non-{@code null}) class loader for the given class context.
     *
     * @param classContext the class context (must not be {@code null})
     * @return the class loader (not {@code null}, may be the bootstrap class loader)
     */
    VmClassLoader getClassLoaderForContext(ClassContext classContext);

    /**
     * Register an executable element that will be directly handled by the given invokable.
     *
     * @param element the element (must not be {@code null})
     * @param invokable the invokable (must not be {@code null})
     */
    void registerInvokable(ExecutableElement element, VmInvokable invokable);

    /**
     * Get the primitive class for the given primitive identifier.
     *
     * @param primitive the primitive identifier (must not be {@code null})
     * @return the primitive class (not {@code null})
     */
    VmPrimitiveClass getPrimitiveClass(Primitive primitive);

    /**
     * Create an instance of {@code java.lang.invoke.MethodType} corresponding to the given descriptor.
     *
     * @param classContext the class context (must not be {@code null})
     * @param methodDescriptor the method descriptor (must not be {@code null})
     * @return the object instance (not {@code null})
     */
    VmObject createMethodType(ClassContext classContext, MethodDescriptor methodDescriptor);

    /**
     * Create a method handle from the given constant.
     *
     * @param classContext the class context (must not be {@code null})
     * @param constant the method handle constant (must not be {@code null})
     * @return the method handle instance (not {@code null})
     * @throws Thrown when the method handle creation throws an exception
     */
    VmObject createMethodHandle(ClassContext classContext, MethodHandleConstant constant) throws Thrown;

    /**
     * Create an {@code Object} box for the given literal, returning it as a {@code VmObject}.
     *
     * @param classContext the class context (must not be {@code null})
     * @param literal the literal value (must not be {@code null})
     * @return the box object
     */
    VmObject box(ClassContext classContext, Literal literal);

    /**
     * Create a new reference array with the given element type.
     *
     * @param elementType the element type (must not be {@code null})
     * @param size the array size
     * @return the new array (not {@code null})
     */
    VmReferenceArray newArrayOf(VmClass elementType, int size);

    /**
     * Get or create the full-powered {@link java.lang.invoke.MethodHandles.Lookup} object for the given class.
     *
     * @param vmClass the class (must not be {@code null})
     * @return the lookup object (not {@code null})
     */
    VmObject getLookup(VmClass vmClass);

    /**
     * Get the complete list of threads which were started from within interpreted code.
     *
     * @return the thread list
     */
    VmThread[] getStartedThreads();
}
