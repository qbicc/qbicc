package org.qbicc.interpreter;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.Primitive;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import io.smallrye.common.constraint.Assert;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
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
     * Perform second-stage VM initialization in the currently attached thread.
     */
    void initialize2();

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
     * Load a resource. Resources are not cached. The current thread must be bound to a VM thread.
     *
     * @param classContext the class context instance (must not be {@code null})
     * @param name the resource name (must not be {@code null})
     * @return the resource bytes, or {@code null} if it is not found
     * @throws Thrown if the internal JVM has thrown an exception while loading the resource
     */
    byte[] loadResource(ClassContext classContext, String name) throws Thrown;

    /**
     * Load all resources with the given name. Resources are not cached. The current thread must be bound to a VM thread.
     *
     * @param classContext the class context instance (must not be {@code null})
     * @param name the resource name (must not be {@code null})
     * @return the list of resource contents as bytes, (not {@code null})
     * @throws Thrown if the internal JVM has thrown an exception while loading the resource
     */
    List<byte[]> loadResources(ClassContext classContext, String name) throws Thrown;

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
     * Construct a new instance of the given class using the given constructor, initializing the class if necessary.
     *
     * @param vmClass the class (must not be {@code null})
     * @param constructor the constructor (must not be {@code null} and must match the class)
     * @param arguments the constructor arguments
     * @return the new instance (not {@code null})
     * @throws Thrown if an exception is thrown from the constructor
     * @throws IllegalArgumentException if the class or constructor is not valid for construction
     */
    VmObject newInstance(VmClass vmClass, ConstructorElement constructor, List<Object> arguments) throws Thrown;

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
     * Allocate memory of the given dimensions. The memory may be strongly typed (i.e. storing values that do
     * not correspond to valid members of the given type may throw an exception).
     *
     * @param type the type to allocate
     * @param count the number of copies of the type
     * @return the memory
     */
    Memory allocate(ValueType type, long count);

    VmClass getClassForDescriptor(VmClassLoader cl, TypeDescriptor descriptor);

    /**
     * Intern a host JVM String as a VmString
     * @param string the host string to intern
     * @return the interned VmString
     */
    VmString intern(String string);

    /**
     * Is the given VmObject and interned VmString?
     */
    boolean isInternedString(VmObject val);

    /**
     * Iterate over all interned VmStrings
     * @param thunk the function to apply to each VmString
     */
    void forEachInternedString(Consumer<VmString> thunk);

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
     * Register invocation hooks for methods on an interpreted class.
     * Any call to the original method will instead be redirected to the hook.
     * <p>
     * An instance of the given hook class will be constructed using its single constructor.
     *
     * @param clazz the class containing the methods to hook (must not be {@code null})
     * @param hookClass the hook class (must not be {@code null})
     * @param lookup a lookup that grants sufficient access to {@code hookClass} to be able to invoke all of its hook methods (must not be {@code null})
     * @throws IllegalArgumentException if the hook methods do not match methods on {@code clazz},
     *      if the hook class does not contain exactly one constructor,
     *      or if the given {@code lookup} does not grant access to all the hook methods on the class
     */
    void registerHooks(VmClass clazz, Class<?> hookClass, MethodHandles.Lookup lookup) throws IllegalArgumentException;

    /**
     * Register invocation hooks for methods on an interpreted class in the manner of
     * {@link #registerHooks(VmClass, Class, MethodHandles.Lookup)}, where
     * the hook methods and constructor must be {@code public}.
     *
     * @param clazz the class containing the methods to hook (must not be {@code null})
     * @param hookClass the hook class (must not be {@code null})
     * @throws IllegalArgumentException if the hook methods do not match methods on {@code clazz},
     *      if the hook class does not contain exactly one public constructor,
     *      or if any of the hook methods on the class are non-public
     */
    default void registerHooks(VmClass clazz, Class<?> hookClass) throws IllegalArgumentException {
        registerHooks(clazz, hookClass, MethodHandles.publicLookup());
    }

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
     * Create a "thin" box for the given literal as an {@code Object}.
     *
     * @param classContext the class context (must not be {@code null})
     * @param literal the literal value (must not be {@code null})
     * @return the box object
     */
    Object boxThin(ClassContext classContext, Literal literal);

    /**
     * Create a new reference array with the given element type.
     *
     * @param elementType the element type (must not be {@code null})
     * @param size the array size
     * @return the new array (not {@code null})
     */
    VmReferenceArray newArrayOf(VmClass elementType, int size);

    VmReferenceArray newArrayOf(VmClass elementType, VmObject[] array);

    VmArray newByteArray(byte[] array);

    VmArray newCharArray(char[] array);

    VmArray newDoubleArray(double[] array);

    VmArray newFloatArray(float[] array);

    VmArray newIntArray(int[] array);

    VmArray newLongArray(long[] array);

    VmArray newShortArray(short[] array);

    VmArray newBooleanArray(boolean[] array);

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
