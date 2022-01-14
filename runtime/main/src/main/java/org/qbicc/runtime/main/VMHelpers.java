package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoSideEffects;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
@SuppressWarnings("unused")
public final class VMHelpers {

    @NoSideEffects
    @Hidden
    public static boolean instanceofClass(Object instance, Class<?> cls) {
        if (instance == null) {
            return false;
        }
        type_id toTypeId = CompilerIntrinsics.getTypeIdFromClass(cls);
        uint8_t toDim = CompilerIntrinsics.getDimensionsFromClass(cls);
        return isAssignableTo(instance, toTypeId, toDim);
    }

    @NoSideEffects
    @Hidden
    public static boolean instanceofTypeId(Object instance, type_id typeId, uint8_t dimensions) {
        if (instance == null) {
            return false;
        }
        return isAssignableTo(instance, typeId, dimensions);
    }

    @Hidden
    public static void arrayStoreCheck(Object value, type_id toTypeId, uint8_t toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        raiseArrayStoreException();
    }

    @Hidden
    public static void checkcastClass(Object value, Class<?> cls) {
        type_id toTypeId = CompilerIntrinsics.getTypeIdFromClass(cls);
        uint8_t toDim = CompilerIntrinsics.getDimensionsFromClass(cls);
        checkcastTypeId(value, toTypeId, toDim);
    }

    @Hidden
    public static void checkcastTypeId(Object value, type_id toTypeId, uint8_t toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        throw new ClassCastException();
    }

    // Invariant: value is not null
    @NoSideEffects
    @Hidden
    public static boolean isAssignableTo(Object value, type_id toTypeId, uint8_t toDimensions) {
        type_id fromTypeId = CompilerIntrinsics.typeIdOf(value);
        if (CompilerIntrinsics.isReferenceArray(fromTypeId)) {
            return isTypeIdAssignableTo(CompilerIntrinsics.elementTypeIdOf(value), CompilerIntrinsics.dimensionsOf(value), toTypeId, toDimensions);
        } else {
            return isTypeIdAssignableTo(fromTypeId, zero(), toTypeId, toDimensions);
        }
    }

    @NoSideEffects
    @Hidden
    public static boolean isTypeIdAssignableTo(type_id fromTypeId, uint8_t fromDimensions, type_id toTypeId, uint8_t toDimensions) {
        return fromDimensions == toDimensions && isAssignableToLeaf(fromTypeId, toTypeId)
            || fromDimensions.isGt(toDimensions) && isAssignableToLeaf(CompilerIntrinsics.getReferenceArrayTypeId(), toTypeId);
    }

    @NoSideEffects
    @Hidden
    private static boolean isAssignableToLeaf(type_id fromTypeId, type_id toTypeId) {
        if (CompilerIntrinsics.isPrimitive(toTypeId) || CompilerIntrinsics.isPrimitive(fromTypeId)) {
            return false;
        } else if (CompilerIntrinsics.isInterface(toTypeId)) {
            return CompilerIntrinsics.doesImplement(fromTypeId, toTypeId);
        } else {
            // in the physical type range
            return toTypeId.isLe(fromTypeId) && fromTypeId.isLe(CompilerIntrinsics.maxSubClassTypeIdOf(toTypeId));
        }
    }

    @Hidden
    public static Class<?> getClassFromObject(Object instance) {
        type_id typeId = CompilerIntrinsics.typeIdOf(instance);
        uint8_t dims = word(0);
        if (CompilerIntrinsics.isReferenceArray(typeId)) {
            typeId = CompilerIntrinsics.elementTypeIdOf(instance);
            dims = CompilerIntrinsics.dimensionsOf(instance);
        }
        return getClassFromTypeid(typeId, dims);
    }

    @NoSideEffects
    @Hidden
    @Inline(InlineCondition.NEVER)
    static Class<?> getClassFromTypeid(type_id typeId, uint8_t dimensions) {
        return CompilerIntrinsics.getClassFromTypeId(typeId, dimensions);
    }

    @Hidden
    @Deprecated // moved to Class$_native.getSuperClass
    static Class<?> getSuperClass(type_id typeId) {
        if (CompilerIntrinsics.isJavaLangObject(typeId) || CompilerIntrinsics.isPrimitive(typeId) || CompilerIntrinsics.isInterface(typeId)) {
            return null;
        }
        type_id superTypeId = CompilerIntrinsics.getSuperClassTypeId(typeId);
        uint8_t dims = word(0);
        return getClassFromTypeid(superTypeId, dims);
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseAbstractMethodError() {
        throw new AbstractMethodError();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseArithmeticException() {
        throw new ArithmeticException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseArrayIndexOutOfBoundsException() {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseArrayStoreException() {
        throw new ArrayStoreException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseClassCastException() {
        throw new ClassCastException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseIncompatibleClassChangeError() {
        throw new IncompatibleClassChangeError(); 
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseNegativeArraySizeException() {
        throw new NegativeArraySizeException(); 
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseNullPointerException() {
        throw new NullPointerException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    static void raiseUnsatisfiedLinkError(String target) {
        throw new UnsatisfiedLinkError(target);
    }
 

    // Temporary helper methods to print to stdout
    // TODO: remove these once we have better runtime trace facilities

    @extern
    public static native int putchar(int arg);


    // Run time class loading

    public static Class<?> classForName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        // TODO: keep a map of run time loadable classes per class loader
        throw new ClassNotFoundException("Run time class loading not yet supported");
    }

    /**
     * Wrapper for threadWrapperNative intrinsic.
     * The export annotation allows this function to be passed as the void*(void*) type required by pthread_create
     * and doesn't generate an additional thread parameter.
     * @param threadParam - java.lang.Thread object that has been cast to a void pointer to be compatible with pthread_create
     * @return null - this return value will not be used
     */
    @export
    public static void_ptr threadWrapper(void_ptr threadParam) {
        if (threadParam == null) {
            // TODO this is a workaround until addr_of_function is working
            return threadParam;
        }
        return CompilerIntrinsics.threadWrapperNative(threadParam);
    }

    /**
     * Helper for java.lang.Thread.start0 allocates a pthread and creates/runs the thread.
     * @param runFuncPtr - pointer to threadWrapper
     * @param thread - Java thread object that has been cast to a void pointer to be compatible with pthread_create
     */
    @Hidden
    @Inline(InlineCondition.NEVER)
    public static void JLT_start0(void_ptr_unaryoperator_function_ptr runFuncPtr, void_ptr thread) {
        // TODO once invokedynamic is working for lambda expressions use addr_of_function to fetch the correct function pointer
        //function_ptr<UnaryOperator<void_ptr>> runFuncPtr = addr_of_function(VMHelpers::threadWrapperNative);

        /* create pthread */
        ptr<?> pthreadVoid = malloc(sizeof(pthread_t.class));
        if (pthreadVoid.isNull()) {
            throw new OutOfMemoryError(/*"Allocation failed"*/);
        }
        ptr<pthread_t> pthreadPtr = (ptr<pthread_t>) castPtr(pthreadVoid, pthread_t.class);

        /* make GC aware of thread before starting */
        if (!CompilerIntrinsics.saveNativeThread(thread, (pthread_t_ptr) pthreadPtr)) {
            free(pthreadVoid);
            throw new OutOfMemoryError();
        }

        int result = pthread_create((pthread_t_ptr) pthreadPtr, (const_pthread_attr_t_ptr)null, runFuncPtr, thread).intValue();
        if (0 != result) {
            free(pthreadVoid);
            throw new InternalError("pthread error code: " + result);
        }
    }

    // TODO: Class library compatibility kludge; remove once qbicc 0.3.0 is released and CoreIntrnsic for Thread.start0 is updated.
    public static void_ptr threadWrapperNative(void_ptr threadParam) {
        return CompilerIntrinsics.threadWrapperNative(threadParam);
    }
}
