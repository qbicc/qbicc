package org.qbicc.runtime.main;

import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.deserialization.HeapDeserializationError;
import org.qbicc.runtime.stdc.Stddef;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
@SuppressWarnings("unused")
public final class VMHelpers {
    /* map Java object to native mutex for object monitor bytecodes. */
    static ConcurrentMap<Object, NativeObjectMonitor> objectMonitorNatives = null;

    @NoSideEffects
    public static boolean instanceof_class(Object instance, Class<?> cls) {
        if (instance == null) {
            return false;
        }
        type_id toTypeId = ObjectModel.get_type_id_from_class(cls);
        int toDim = ObjectModel.get_dimensions_from_class(cls);
        return isAssignableTo(instance, toTypeId, toDim);
    }

    @NoSideEffects
    public static boolean instanceof_typeId(Object instance, type_id typeId, int dimensions) {
        if (instance == null) {
            return false;
        }
        return isAssignableTo(instance, typeId, dimensions);
    }

    public static void arrayStoreCheck(Object value, type_id toTypeId, int toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        raiseArrayStoreException();
    }

    public static void checkcast_class (Object value, Class<?> cls) {
        type_id toTypeId = ObjectModel.get_type_id_from_class(cls);
        int toDim = ObjectModel.get_dimensions_from_class(cls);
        checkcast_typeId(value, toTypeId, toDim);
    }

    public static void checkcast_typeId(Object value, type_id toTypeId, int toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        throw new ClassCastException();
    }

    // Invariant: value is not null
    @NoSideEffects
    private static boolean isAssignableTo(Object value, type_id toTypeId, int toDimensions) {
        type_id valueTypeId = ObjectModel.type_id_of(value);
        if (toDimensions == 0) {
            return isAssignableToLeaf(valueTypeId, toTypeId);
        } else if (ObjectModel.is_reference_array(valueTypeId)) {
            int valueDims = ObjectModel.dimensions_of(value);
            if (valueDims == toDimensions) {
                return isAssignableToLeaf(ObjectModel.element_type_id_of(value), toTypeId);
            } else if (valueDims > toDimensions) {
                return ObjectModel.is_java_lang_object(toTypeId) ||
                    ObjectModel.is_java_io_serializable(toTypeId) ||
                    ObjectModel.is_java_lang_cloneable(toTypeId);
            }
        }
        return false;
    }

    @NoSideEffects
    private static boolean isAssignableToLeaf(type_id valueTypeId, type_id toTypeId) {
        if (ObjectModel.is_class(toTypeId)) {
            type_id maxTypeId = ObjectModel.max_subclass_type_id_of(toTypeId);
            return toTypeId.intValue() <= valueTypeId.intValue() && valueTypeId.intValue() <= maxTypeId.intValue();
        } else if (ObjectModel.is_interface(toTypeId)) {
            return ObjectModel.does_implement(valueTypeId, toTypeId);
        } else {
            // PrimitiveObjectArray
            return valueTypeId == toTypeId;
        }
    }

    // TODO: mark this with a "NoInline" annotation
    @NoSideEffects
    static Class<?> classof_from_typeid(type_id typeId) {
        // Load the java.lang.Class object from an array of them indexed by typeId.
        return null; // TODO: Implement this! (or perhaps implement it inline; it should take less code than a call).
    }

    @NoSideEffects
    private static void omError(c_int nativeErrorCode) throws IllegalMonitorStateException {
        int errorCode = nativeErrorCode.intValue();
        if (0 != errorCode) {
            throw new IllegalMonitorStateException("error code is: " + errorCode);
        }
    }

    // TODO: mark this with a "NoInline" annotation
    static void monitor_enter(Object object) throws IllegalMonitorStateException {
        /* TODO: deal with racy nature of this creation */
        if (objectMonitorNatives == null) {
            objectMonitorNatives = new ConcurrentHashMap<>();
        }

        // TODO malloc(sizeof(class)) resulted in "invalid coercion of s64 to u64" this is a workaround
        Stddef.size_t mutexAttrSize = sizeof(pthread_mutexattr_t.class);
        ptr<?> attrVoid = malloc(word(mutexAttrSize.longValue()));
        if (attrVoid.isNull()) {
            throw new OutOfMemoryError(/*"Allocation failed"*/);
        }
        ptr<pthread_mutexattr_t> attr = (ptr<pthread_mutexattr_t>)castPtr(attrVoid, pthread_mutexattr_t.class);

        Stddef.size_t mutexSize = sizeof(pthread_mutex_t.class);
        ptr<?> mVoid = malloc(word(mutexSize.longValue()));
        if (attrVoid.isNull()) {
            throw new OutOfMemoryError(/*"Allocation failed"*/);
        }
        ptr<pthread_mutex_t> m = (ptr<pthread_mutex_t>)castPtr(mVoid, pthread_mutex_t.class);

        omError(pthread_mutexattr_init((pthread_mutexattr_t_ptr)attr));
        omError(pthread_mutexattr_settype((pthread_mutexattr_t_ptr)attr, PTHREAD_MUTEX_RECURSIVE));
        omError(pthread_mutex_init((pthread_mutex_t_ptr)m, (const_pthread_mutexattr_t_ptr)attr));
        omError(pthread_mutexattr_destroy((pthread_mutexattr_t_ptr)attr));
        free(attrVoid);

        // TODO acquire monitor and add to hash map
    }

    // TODO: mark this with a "NoInline" annotation
    static void monitor_exit(Object object) throws IllegalMonitorStateException {
//        NativeObjectMonitor monitor = objectMonitorNatives.get(object);
//        if (null == monitor) {
//            throw new IllegalMonitorStateException("monitor could not be found for monitorexit");
//        }
//        omError(pthread_mutex_unlock(monitor.getPthreadMutex()));
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseAbstractMethodError() {
        throw new AbstractMethodError();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseArithmeticException() {
        throw new ArithmeticException();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseArrayIndexOutOfBoundsException() {
        throw new ArrayIndexOutOfBoundsException();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseArrayStoreException() {
        throw new ArrayStoreException();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseClassCastException() {
        throw new ClassCastException();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseHeapDeserializationError() {
        throw new HeapDeserializationError();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseIncompatibleClassChangeError() { throw new IncompatibleClassChangeError(); }

    // TODO: mark this with a "NoInline" annotation
    static void raiseNegativeArraySizeException() { throw new NegativeArraySizeException(); }

    // TODO: mark this with a "NoInline" annotation
    static void raiseNullPointerException() {
        throw new NullPointerException();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseUnsatisfiedLinkError() { throw new UnsatisfiedLinkError(); }
}
