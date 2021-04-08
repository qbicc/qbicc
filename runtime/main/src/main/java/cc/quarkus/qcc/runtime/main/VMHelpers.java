package cc.quarkus.qcc.runtime.main;

import cc.quarkus.qcc.runtime.CNative;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.PThread.*;
import static cc.quarkus.qcc.runtime.stdc.Stdlib.*;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
@SuppressWarnings("unused")
public final class VMHelpers {

    public static boolean instanceof_class(Object instance, Class<?> cls) {
        if (instance == null) {
            return false;
        }
        type_id toTypeId = ObjectModel.get_type_id_from_class(cls);
        int toDim = ObjectModel.get_dimensions_from_class(cls);
        return isAssignableTo(instance, toTypeId, toDim);
    }

    public static boolean instanceof_typeId(Object instance, CNative.type_id typeId, int dimensions) {
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
    static Class<?> classof_from_typeid(type_id typeId) {
        // Load the java.lang.Class object from an array of them indexed by typeId.
        return null; // TODO: Implement this! (or perhaps implement it inline; it should take less code than a call).
    }

    private static void omError(c_int nativeErrorCode) throws IllegalMonitorStateException {
        int errorCode = nativeErrorCode.intValue();
        if (0 != errorCode) {
            throw new IllegalMonitorStateException("error code is: " + errorCode);
        }
    }

    // TODO: mark this with a "NoInline" annotation
    static void monitorEnter(Object object) throws IllegalMonitorStateException {
        /* get native mutex associated with object, else atomically create a new one. */
        NativeObjectMonitor monitor = Main.objectMonitorNatives.computeIfAbsent(object, k -> {
                pthread_mutexattr_t_ptr attr = malloc(sizeof(pthread_mutexattr_t.class));
                pthread_mutex_t_ptr m = malloc(sizeof(pthread_mutex_t.class));
                omError(pthread_mutexattr_init(attr));
                /* recursive lock allows for locking multiple times on the same thread. */
                omError(pthread_mutexattr_settype(attr, PTHREAD_MUTEX_RECURSIVE));
                omError(pthread_mutex_init(m, attr.cast()));
                omError(pthread_mutexattr_destroy(attr));
                free(attr);
                return new NativeObjectMonitor(m);
            }
        );
        omError(pthread_mutex_lock(monitor.getPthreadMutex().cast()));
    }

    // TODO: mark this with a "NoInline" annotation
    static void monitorExit(Object object) throws IllegalMonitorStateException {
        NativeObjectMonitor monitor = Main.objectMonitorNatives.get(object);
        if (null == monitor) {
            throw new IllegalMonitorStateException("monitor could not be found for monitorexit");
        }
        omError(pthread_mutex_unlock(monitor.getPthreadMutex().cast()));
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
