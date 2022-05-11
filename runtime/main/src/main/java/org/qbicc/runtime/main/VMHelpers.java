package org.qbicc.runtime.main;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.NotReachableException;

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
    @AutoQueued
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
    @AutoQueued
    public static boolean instanceofTypeId(Object instance, type_id typeId, uint8_t dimensions) {
        if (instance == null) {
            return false;
        }
        return isAssignableTo(instance, typeId, dimensions);
    }

    @Hidden
    @AutoQueued
    public static void arrayStoreCheck(Object value, type_id toTypeId, uint8_t toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        raiseArrayStoreException();
    }

    @Hidden
    @AutoQueued
    public static void checkcastClass(Object value, Class<?> cls) {
        type_id toTypeId = CompilerIntrinsics.getTypeIdFromClass(cls);
        uint8_t toDim = CompilerIntrinsics.getDimensionsFromClass(cls);
        checkcastTypeId(value, toTypeId, toDim);
    }

    @Hidden
    @AutoQueued
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
    @AutoQueued
    public static Class<?> getClassFromObject(Object instance) {
        type_id typeId = CompilerIntrinsics.typeIdOf(instance);
        uint8_t dims = word(0);
        if (CompilerIntrinsics.isReferenceArray(typeId)) {
            typeId = CompilerIntrinsics.elementTypeIdOf(instance);
            dims = CompilerIntrinsics.dimensionsOf(instance);
        }
        return CompilerIntrinsics.getClassFromTypeId(typeId, dims);
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseAbstractMethodError() {
        throw new AbstractMethodError();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseArithmeticException() {
        throw new ArithmeticException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseArrayIndexOutOfBoundsException() {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseArrayStoreException() {
        throw new ArrayStoreException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseClassCastException() {
        throw new ClassCastException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseIncompatibleClassChangeError() {
        throw new IncompatibleClassChangeError(); 
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseNegativeArraySizeException() {
        throw new NegativeArraySizeException(); 
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseNullPointerException() {
        throw new NullPointerException();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseUnsatisfiedLinkError(String target) {
        throw new UnsatisfiedLinkError(target);
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseUnreachableCodeError(String target) {
        throw new NotReachableException(target);
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
    @AutoQueued
    public static void JLT_start0(void_ptr_unaryoperator_function_ptr runFuncPtr, void_ptr thread) {
        // TODO once invokedynamic is working for lambda expressions use addr_of_function to fetch the correct function pointer
        //function_ptr<UnaryOperator<void_ptr>> runFuncPtr = addr_of_function(VMHelpers::threadWrapperNative);

        /* create pthread */
        pthread_t_ptr pthreadPtr = malloc(sizeof(pthread_t.class));
        if (pthreadPtr.isNull()) {
            throw new OutOfMemoryError(/*"Allocation failed"*/);
        }

        /* make GC aware of thread before starting */
        if (!CompilerIntrinsics.saveNativeThread(thread, pthreadPtr)) {
            free(pthreadPtr.cast());
            throw new OutOfMemoryError();
        }

        int result = pthread_create(pthreadPtr, zero(), runFuncPtr, thread).intValue();
        if (0 != result) {
            free(pthreadPtr.cast());
            throw new InternalError("pthread error code: " + result);
        }
    }

    // TODO: Class library compatibility kludge.
    //       Once Thread.start0 is defined in Thread$_native instead as a compiler intrinsic, it can
    //       call CompilerIntrinsics.threadWrapperNative directly and this redirection method can be removed.
    public static void_ptr threadWrapperNative(void_ptr threadParam) {
        return CompilerIntrinsics.threadWrapperNative(threadParam);
    }
}
