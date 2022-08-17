package org.qbicc.runtime.main;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.NotReachableException;

import java.util.Arrays;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

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
     * A pointer to this function is passed to pthread_create in Thread.start0.
     * The role of this wrapper is to transition a newly started Thread from C to Java calling
     * conventions and then invoke the Thread's run method.
     * @param threadParam - java.lang.Thread object for the newly started thread (cast to a void pointer to be compatible with pthread_create)
     * @return null - this return value will not be used
     */
    @export
    public static void_ptr pthreadCreateWrapper(void_ptr threadParam) {
        Object thrObj = ptrToRef(threadParam);
        Thread thread = (Thread)thrObj;
        VM._qbicc_bound_thread = thread;
        thread.run();
        return word(0).cast();
    }

    /**
     * Return the Class instance corresponding to the given name and loader.
     * @param name The internal name of the class
     * @param loader The defining loader of the class (null for bootloader)
     * @return The requested Class instance or <code>null</code> if it is not found
     */
    public static Class<?> findLoadedClass(String name, ClassLoader loader) {
        if (loader == null) {
            int idx = Arrays.binarySearch(InitialHeap.bootstrapClassNames, name);
            return idx >= 0 ? InitialHeap.bootstrapClasses[idx] : null;
        }
        // TODO: Extend lookup structures to support additional classloaders
        return null;
    }
}
