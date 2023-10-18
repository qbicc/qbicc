package org.qbicc.runtime.main;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.NotReachableException;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;

import java.util.Arrays;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
@SuppressWarnings("unused")
public final class VMHelpers {

    @NoSideEffects
    @SafePoint(SafePointBehavior.NONE)
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
    @SafePoint(SafePointBehavior.NONE)
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
    @SafePoint(SafePointBehavior.NONE)
    @Hidden
    public static boolean isAssignableTo(Object value, type_id toTypeId, uint8_t toDimensions) {
        type_id fromTypeId = CompilerIntrinsics.typeIdOf(value);
        if (CompilerIntrinsics.isReferenceArray(fromTypeId)) {
            return isTypeIdAssignableTo(CompilerIntrinsics.elementTypeIdOf(value), CompilerIntrinsics.dimensionsOf(value), toTypeId, toDimensions);
        } else {
            return isTypeIdAssignableTo(fromTypeId, zero(), toTypeId, toDimensions);
        }
    }

    @SafePoint(SafePointBehavior.NONE)
    @NoSideEffects
    @Hidden
    public static boolean isTypeIdAssignableTo(type_id fromTypeId, uint8_t fromDimensions, type_id toTypeId, uint8_t toDimensions) {
        return fromDimensions == toDimensions && isAssignableToLeaf(fromTypeId, toTypeId)
            || fromDimensions.isGt(toDimensions) && isAssignableToLeaf(CompilerIntrinsics.getReferenceArrayTypeId(), toTypeId);
    }

    @SafePoint(SafePointBehavior.NONE)
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
    static void raiseUnsatisfiedLinkErrorDispatchStub() {
        throw new UnsatisfiedLinkError();
    }

    @Hidden
    @NoReturn
    @Inline(InlineCondition.NEVER)
    @AutoQueued
    static void raiseUnreachableCodeError(String target) {
        throw new NotReachableException(target);
    }

    /**
     * Return the Class instance corresponding to the given name and loader.
     * @param name The internal name of the class
     * @param loader The defining loader of the class (null for bootloader)
     * @return The requested Class instance or <code>null</code> if it is not found
     */
    public static Class<?> findLoadedClass(String name, ClassLoader loader) {
        int loaderIndex = -1;
        if (loader == null) {
            loaderIndex = 0;
        } else {
            for (int i=0; i<InitialHeap.classLoaders.length; i++) {
                if (InitialHeap.classLoaders[i] == loader) {
                    loaderIndex = i;
                    break;
                }
            }
        }
        if (loaderIndex == -1) {
            return null;
        }
        int idx = Arrays.binarySearch(InitialHeap.classNames[loaderIndex], name);
        return idx >= 0 ? InitialHeap.classes[loaderIndex][idx] : null;
    }
}
