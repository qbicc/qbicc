package org.qbicc.runtime.main;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoReturn;
import org.qbicc.runtime.NoSideEffects;
import org.qbicc.runtime.posix.PThread;
import org.qbicc.runtime.stdc.Stddef;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.PThread.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;

/**
 * Runtime Helpers to support the operation of the compiled code.
 */
@SuppressWarnings("unused")
public final class VMHelpers {

    @Hidden
    public static Class<?> getClass(Object instance) {
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

    @NoSideEffects
    @Hidden
    @Inline(InlineCondition.NEVER)
    static Class<?> getClassFromTypeid(type_id typeId, uint8_t dimensions) {
        return CompilerIntrinsics.getClassFromTypeId(typeId, dimensions);
    }

    @Hidden
    static Class<?> getSuperClass(type_id typeId) {
        if (CompilerIntrinsics.isJavaLangObject(typeId) || CompilerIntrinsics.isPrimitive(typeId) || CompilerIntrinsics.isInterface(typeId)) {
            return null;
        }
        type_id superTypeId = CompilerIntrinsics.getSuperClassTypeId(typeId);
        uint8_t dims = word(0);
        return getClassFromTypeid(superTypeId, dims);
    }

    @Hidden
    @Inline(InlineCondition.NEVER)
    static void monitorEnter(Object object) throws IllegalMonitorStateException {
        int result;
        if (object == null) {
            /* TODO skip for now. Object should never be null except that
                classof_from_typeid is not currently implemented. */
            return;
        }
        pthread_mutex_t_ptr nom = CompilerIntrinsics.getNativeObjectMonitor(object);
        if (nom == null) {
            ptr<?> attrVoid = malloc(sizeof(pthread_mutexattr_t.class));
            if (attrVoid.isNull()) {
                throw new OutOfMemoryError(/*"Allocation failed"*/);
            }

            /* free attribute on success or failure, it won't be needed past mutex creation */
            try {
                ptr<pthread_mutexattr_t> attr = (ptr<pthread_mutexattr_t>) castPtr(attrVoid, pthread_mutexattr_t.class);

                result = pthread_mutexattr_init((pthread_mutexattr_t_ptr) attr).intValue();
                if (0 != result) {
                    throw new IllegalMonitorStateException("error code: " + result);
                }
                /* destroy attribute on success or failure, it won't be needed past mutex creation */
                try {
                    result = pthread_mutexattr_settype((pthread_mutexattr_t_ptr) attr, PTHREAD_MUTEX_RECURSIVE).intValue();
                    if (0 != result) {
                        throw new IllegalMonitorStateException("error code: " + result);
                    }

                    Stddef.size_t mutexSize = sizeof(pthread_mutex_t.class);
                    ptr<?> mVoid = malloc(word(mutexSize.longValue()));
                    if (mVoid.isNull()) {
                        throw new OutOfMemoryError(/*"Allocation failed"*/);
                    }

                    ptr<pthread_mutex_t> m = (ptr<pthread_mutex_t>) castPtr(mVoid, pthread_mutex_t.class);
                    result = pthread_mutex_init((pthread_mutex_t_ptr) m, (const_pthread_mutexattr_t_ptr) attr).intValue();
                    if (0 != result) {
                        free(mVoid);
                        throw new IllegalMonitorStateException("error code: " + result);
                    }

                    nom = (pthread_mutex_t_ptr) m;
                    if (!CompilerIntrinsics.setNativeObjectMonitor(object, nom)) {
                        /* atomic assignment failed, mutex has already been initialized for object. */
                        pthread_mutex_destroy(nom);
                        free(mVoid);
                        nom = CompilerIntrinsics.getNativeObjectMonitor(object);
                    }
                } finally {
                    pthread_mutexattr_destroy((pthread_mutexattr_t_ptr) attr);
                }
            } finally {
                free(attrVoid);
            }
        }
        result = pthread_mutex_lock(nom).intValue();
        if (0 != result) {
            throw new IllegalMonitorStateException("error code: " + result);
        }
    }

    @Hidden
    @Inline(InlineCondition.NEVER)
    static void monitorExit(Object object) throws IllegalMonitorStateException {
        if (object == null) {
            /* TODO skip for now. Object should never be null except that
                classof_from_typeid is not currently implemented. */
            return;
        }
        pthread_mutex_t_ptr nom = CompilerIntrinsics.getNativeObjectMonitor(object);
        if (nom == null) {
            throw new IllegalMonitorStateException("native monitor could not be found for monitor_exit");
        }
        int result = pthread_mutex_unlock(nom).intValue();
        if (0 != result) {
            throw new IllegalMonitorStateException("error code: " + result);
        }
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
            type_id size = CompilerIntrinsics.getNumberOfTypeIds();
            synchronized(ClinitState.class) {
                if (clinitStates == null) {
                    clinitStates = new ClinitState[size.intValue()];
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

    static void initializeClass(Thread currentThread, type_id typeid) throws Throwable {
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
        if (CompilerIntrinsics.isInitialized(typeid)) {
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
            if (CompilerIntrinsics.isClass(typeid)) {
                try {
                    // Initialize the super class (and its superclasses if not already initialized)
                    if (!CompilerIntrinsics.isJavaLangObject(typeid)) {
                        initializeClass(currentThread, CompilerIntrinsics.getSuperClassTypeId(typeid));
                    }
                    // Initialize the super interfaces, at least those that declare default methods
                    // We calculate if a class has default methods during image build and we elide
                    // this entire process if it doesn't have any default methods as there are no
                    // interfaces that would need initailization.  See LoadedTypeDefinitionImpl's
                    // constructor for where this is calculated
                    if (ObjectModel.hasDefaultMethods(typeid)) {
                        // TODO: The ordering of superinterface intialization isn't correct here as it should start 
                        // recursively based on the directly declared interfaces.  This does all interfaces implemented.
                        type_id interfaceId = CompilerIntrinsics.getFirstInterfaceTypeId();
                        for (int i = 0; i < CompilerIntrinsics.getNumberOfBytesInInterfaceBitsArray(); i++) {
                            byte b = CompilerIntrinsics.getByteOfInterfaceBits(typeid, i);
                            if (b == 0) {
                                // no interfaces in this byte - advance to the next one
                                interfaceId = CNative.<type_id>word(interfaceId.intValue() + 8);
                            } else {
                                // walk the byte to find the interfaces
                                for (int j = 0; j < 8; j++) {
                                    if ((b & 1) == 1) {
                                        if (ObjectModel.declaresDefaultMethods(interfaceId)) {
                                            initializeClass(currentThread, interfaceId);
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
                if (ObjectModel.hasClassInitializer(typeid)) {
                    CompilerIntrinsics.callClassInitializer(typeid);
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
                    CompilerIntrinsics.setInitialized(typeid);
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
        initializeClass(Thread.currentThread(), CompilerIntrinsics.typeIdOf(o));
        setInitialized(o);
        putchar(isInitialized(o) ? 'Y' : 'N');
    }

    public static boolean isInitialized(Object o) throws Throwable {
        return CompilerIntrinsics.isInitialized(CompilerIntrinsics.typeIdOf(o));
    }

    public static void setInitialized(Object o) throws Throwable {
        CompilerIntrinsics.setInitialized(CompilerIntrinsics.typeIdOf(o));
    }

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
