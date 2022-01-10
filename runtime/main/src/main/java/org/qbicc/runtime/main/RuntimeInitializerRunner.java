package org.qbicc.runtime.main;

/**
 * Effectively a lambda that will execute a specific runtime initializer.
 * <p>
 * Runtime init checks are constructed by compile-time instantiation of
 * a Once instance whose Runnable is an instance of this class with the
 * initID of the target initializer.  These instances are then serialized
 * as part of the build time heap.
 */
@SuppressWarnings("unused")
public final class RuntimeInitializerRunner implements Runnable {
    private final int initID;

    private RuntimeInitializerRunner(int initID) {
        this.initID = initID;
    }

    @SuppressWarnings("unused") // invoked at buildtime
    public static Once allocateThunk(int initID) {
        return new Once(new RuntimeInitializerRunner(initID));
    }

    @Override
    public void run() {
        CompilerIntrinsics.callRuntimeInitializer(initID);
    }
}
