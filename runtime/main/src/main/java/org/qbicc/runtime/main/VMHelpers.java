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
    static void raiseIncompatibleClassChangeError() {
        throw new IncompatibleClassChangeError(); 
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseNegativeArraySizeException() {
        throw new NegativeArraySizeException(); 
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseNullPointerException() {
        throw new NullPointerException();
    }

    // TODO: mark this with a "NoInline" annotation
    static void raiseUnsatisfiedLinkError() {
        throw new UnsatisfiedLinkError(); 
    }
 
    // TODO: mark with "must be build time initialized" annotation
    static class ClinitState {
        static final byte STATE_UNINITIALIZED = 0;
        static final byte STATE_INPROGRESS = 1;
        static final byte STATE_INITIALIZED = 2;
        static final byte STATE_FAILED = 3;

        Thread initializerThread;
        Throwable errorInInitializer;
        // Must be one of:
        // 0 - uninitialized
        // 1 - in-progress
        // 2 - initialized
        // 3 - failed
        byte initializedState;
        /* TODO: once we have Class objects for every class with a typeid, move
         * this state to the Class itself
         */

        ClinitState(Thread currentThread) {
            initializerThread = currentThread;
            initializedState = STATE_UNINITIALIZED;
        }

        boolean beingInitializedByMe(Thread currentThread) {
            return currentThread == initializerThread;
        }

        boolean isInitialized() {
            return initializedState == STATE_INITIALIZED;
        }

        boolean isInProgress() {
            return initializedState == STATE_INPROGRESS;
        }

        boolean isFailed() {
            return initializedState == STATE_FAILED;
        }

        void setInProgress() {
            assert initializedState == STATE_UNINITIALIZED;
            initializedState = STATE_INPROGRESS;
        }

        void setFailed(Throwable t) {
            errorInInitializer = t;
            initializedState = STATE_FAILED;
        }

        void setInitialized() {
            initializedState = STATE_INITIALIZED;
            initializerThread = null;
        }

        /**
         * Is initialization in a final state?
         * Both Failed and Initialized are final states
         * 
         * @return true if it is, false otherwise
         */
        boolean isInTerminalInitializedState() {
            switch(initializedState) {
                case STATE_FAILED:
                case STATE_INITIALIZED:
                    return true;
                default:
                    return false;
            }
        }

        Throwable initializationAlreadyFailed(type_id typeid) {
            // TODO: convert type_id to a class name
            NoClassDefFoundError e = new NoClassDefFoundError("initialization failed: " + typeid.intValue());
            e.initCause(errorInInitializer);
            throw e;
        }
    }
    
    // This probably needs to be an Object[] or we need to force ClinitState to be
    // initialized at build time or there will be recursive issues - initializing
    // depends on the array, but it depends on a not-yet initialized class.
    // Eventually, array and elements will be build time intialized with null
    // elements for classes that are already initailized.  Makes for a cheaper
    // check as elements can be nulled when initialized.
    static ClinitState[] clinitStates;

    static void initialize_class(Thread currentThread, type_id typeid) throws Throwable {
        ClinitState state;
        // ==== Code in this section would be better done at build time
        if (clinitStates == null) {
            int size = ObjectModel.get_number_of_typeids();
            synchronized(ClinitState.class) {
                if (clinitStates == null) {
                    clinitStates = new ClinitState[size];
                }
            }
        }
        int typeid_value = typeid.intValue();
        ClinitState arrayState = clinitStates[typeid_value];
        if (arrayState == null) {
            state = new ClinitState(currentThread);
            synchronized(clinitStates) {
                arrayState = clinitStates[typeid_value];
                if (arrayState == null) {
                    clinitStates[typeid_value] = state;
                } else {
                    state = arrayState;
                }
            }
        } else {
            state = arrayState;
        }
        // ==== end better at build time section
        assert state != null;
        boolean wasInterrupted = false;
        try {
            synchronized(state) { // state is the "LC"
                if (state.isInProgress()) {
                    if (!state.beingInitializedByMe(currentThread)) {
                        while (!state.isInTerminalInitializedState()) {
                            // `C` is being initialized by another thread, wait on it
                            try {
                                state.wait();
                            } catch (InterruptedException e) {
                                // Don't repeat, keep waiting
                                wasInterrupted = true;
                            }
                        }
                    } else {
                        assert !state.isInTerminalInitializedState();
                        // curentThread is in the process of initializing,
                        // complete normally as this is a recursive request
                        return;
                    }
                }
                
                // Not an else if as both the current initializing thread and waiting
                // threads need to execute this section
                if (state.isInitialized()) {
                    // Successfully initialized, complete normally
                    return;
                } else if (state.isFailed()) {
                    // release and throw NoClassDefFoundError
                    throw state.initializationAlreadyFailed(typeid);
                } else {
                    assert state.beingInitializedByMe(currentThread);
                    state.setInProgress();
                }
            }
            // LC is released at this point, and in-progress by current thread
            // Static field preparation happens at build time
            if (ObjectModel.is_class(typeid)) {
                try {
                    if (!ObjectModel.is_java_lang_object(typeid)) {
                        initialize_class(currentThread, ObjectModel.get_superclass_typeid(typeid));
                    }
                    // TODO: initialize the super interfaces - may be issues with ordering requirements depending on the data we preserve  
                } catch (Throwable t) {
                    synchronized(state) {
                        state.setFailed(t);
                        state.notifyAll();;
                    }
                    throw t;
                }
            }
            // Assertions are never enabled for images, so no check required
            Error clinitThrowable = null;
            try {
                if (ObjectModel.has_class_initializer(typeid)) {
                    ObjectModel.call_class_initializer(typeid);
                }
            } catch (Throwable t) {
                if (t instanceof Error) {
                    clinitThrowable = (Error)t;
                } else {
                    clinitThrowable = new ExceptionInInitializerError(t);
                }
            }
            synchronized(state) {
                if (clinitThrowable == null) {
                    // completed successfully
                    state.setInitialized();
                } else {
                    state.setFailed(clinitThrowable);
                }
                state.notifyAll();
            }
            if (clinitThrowable != null) {
                throw clinitThrowable;
            }
            return;
        } finally {
            /* Interrupted status can only be propagated when the initialization sequence
             * is complete.  Otherwise, classes in progress may be recorded as erroreous.
             */
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
