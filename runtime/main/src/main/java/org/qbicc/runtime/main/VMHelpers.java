package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.stdc.Stddef;
import org.qbicc.runtime.stdc.Stdint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.stdc.Stdint.*;
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
        uint8_t toDim = ObjectModel.get_dimensions_from_class(cls);
        return isAssignableTo(instance, toTypeId, toDim);
    }

    @NoSideEffects
    public static boolean instanceof_typeId(Object instance, type_id typeId, uint8_t dimensions) {
        if (instance == null) {
            return false;
        }
        return isAssignableTo(instance, typeId, dimensions);
    }

    public static void arrayStoreCheck(Object value, type_id toTypeId, uint8_t toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        raiseArrayStoreException();
    }

    public static void checkcast_class (Object value, Class<?> cls) {
        type_id toTypeId = ObjectModel.get_type_id_from_class(cls);
        uint8_t toDim = ObjectModel.get_dimensions_from_class(cls);
        checkcast_typeId(value, toTypeId, toDim);
    }

    public static void checkcast_typeId(Object value, type_id toTypeId, uint8_t toDimensions) {
        if (value == null || isAssignableTo(value, toTypeId, toDimensions)) {
            return;
        }
        throw new ClassCastException();
    }

    // Invariant: value is not null
    @NoSideEffects
    private static boolean isAssignableTo(Object value, type_id toTypeId, uint8_t toDimensions) {
        type_id valueTypeId = ObjectModel.type_id_of(value);
        // putchar('A');
        // putchar(':');
        // printTypeId(valueTypeId);
        // putchar(':');
        // printTypeId(toTypeId);
        // putchar(':');
        // printInt(toDimensions);
        // putchar('\n');
        int intDim = toDimensions.intValue();
        if (intDim == 0) {
            return isAssignableToLeaf(valueTypeId, toTypeId);
        } else if (ObjectModel.is_reference_array(valueTypeId)) {
            uint8_t valueDims = ObjectModel.dimensions_of(value);
            if (valueDims == toDimensions) {
                return isAssignableToLeaf(ObjectModel.element_type_id_of(value), toTypeId);
            } else if (valueDims.intValue() > toDimensions.intValue()) {
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

    static void ensureClinitStatesArray() {
        if (clinitStates == null) {
            int size = ObjectModel.get_number_of_typeids();
            synchronized(ClinitState.class) {
                if (clinitStates == null) {
                    clinitStates = new ClinitState[size];
                }
            }
        }
    }

    // TODO: this should live on the j.l.Class object
    static ClinitState getClinitState(Thread currentThread, type_id typeid) {
        int typeid_value = typeid.intValue();
        ClinitState arrayState = clinitStates[typeid_value];
        if (arrayState != null) {
            return arrayState;
        }
        ClinitState state = new ClinitState(currentThread);
        synchronized(clinitStates) {
            arrayState = clinitStates[typeid_value];
            if (arrayState == null) {
                clinitStates[typeid_value] = state;
            } else {
                state = arrayState;
            }
        }
        return state;
    }

    static void initialize_class(Thread currentThread, type_id typeid) throws Throwable {
        /* Uncomment to trace class init */
        // putchar('I');
        // putchar('N');
        // putchar('I');
        // putchar('T');
        // putchar('<');
        // printTypeId(typeid);
        // putchar('>');
        // putchar('\n');
        ensureClinitStatesArray();
        if (ObjectModel.is_initialized(typeid)) {
            // early exit - is this right for getting the correct memory effects?
            return;
        }
        ClinitState state = getClinitState(currentThread, typeid);
        assert state != null;
        boolean wasInterrupted = false;
        // try {
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
                    // Initialize the super class (and its superclasses if not already initialized)
                    if (!ObjectModel.is_java_lang_object(typeid)) {
                        initialize_class(currentThread, ObjectModel.get_superclass_typeid(typeid));
                    }
                    // Initialize the super interfaces, at least those that declare default methods
                    // We calculate if a class has default methods during image build and we elide
                    // this entire process if it doesn't have any default methods as there are no
                    // interfaces that would need initailization.  See LoadedTypeDefinitionImpl's
                    // constructor for where this is calculated
                    if (ObjectModel.has_default_methods(typeid)) {
                        // TODO: The ordering of superinterface intialization isn't correct here as it should start 
                        // recursively based on the directly declared interfaces.  This does all interfaces implemented.
                        type_id interfaceId = ObjectModel.get_first_interface_typeid();
                        for (int i = 0; i < ObjectModel.get_number_of_bytes_in_interface_bits_array(); i++) {
                            byte b = ObjectModel.get_byte_of_interface_bits(typeid, i);
                            if (b == 0) {
                                // no interfaces in this byte - advance to the next one
                                interfaceId = CNative.<type_id>word(interfaceId.intValue() + 8);
                            } else {
                                // walk the byte to find the interfaces
                                for (int j = 0; j < 8; j++) {
                                    if ((b & 1) == 1) {
                                        if (ObjectModel.declares_default_methods(interfaceId)) {
                                            initialize_class(currentThread, interfaceId);
                                        }
                                    }
                                    b = (byte)(b >> 1);
                                    interfaceId = CNative.<type_id>word(interfaceId.intValue() + 1);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    synchronized(state) {
                        state.setFailed(t);
                        state.notifyAll();
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
                    // update the native data as well for fast checks
                    ObjectModel.set_initialized(typeid);
                } else {
                    state.setFailed(clinitThrowable);
                }
                state.notifyAll();
            }
            if (clinitThrowable != null) {
                throw clinitThrowable;
            }
        // } finally {
        //     /* Interrupted status can only be propagated when the initialization sequence
        //      * is complete.  Otherwise, classes in progress may be recorded as erroreous.
        //      */
        //     if (wasInterrupted) {
        //         Thread.currentThread().interrupt();
        //     }
        // }
        return;
    }

    // Temporary helper methods to print to stdout
    // TODO: remove these once we have better runtime trace facilities

    @extern
    public static native int putchar(int arg);

    static void printTypeId(type_id typeid) {
        int n = typeid.intValue();
        printInt(n);
    }

    static void printInt(int n) {
        char[] numbers = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        boolean seenNonZero = false;
        int divsor = 1000000000;
        do {
            int i = n / divsor;
            if (!seenNonZero && i == 0) {
                // skip
            } else { 
                seenNonZero = true;
                putchar(numbers[i]);
            }
            n %= divsor;
            divsor /= 10;
        } while (divsor != 0);
        if (!seenNonZero) {
            putchar(numbers[0]);
        }
    }

    // Temporary testing method
    public static void testClinit(Object o) throws Throwable {
        putchar(isInitialized(o) ? 'Y' : 'N');
        initialize_class(Thread.currentThread(), ObjectModel.type_id_of(o));
        setInitialized(o);
        putchar(isInitialized(o) ? 'Y' : 'N');
    }

    public static boolean isInitialized(Object o) throws Throwable {
        return ObjectModel.is_initialized(ObjectModel.type_id_of(o));
    }

    public static void setInitialized(Object o) throws Throwable {
        ObjectModel.set_initialized(ObjectModel.type_id_of(o));
    }

}
