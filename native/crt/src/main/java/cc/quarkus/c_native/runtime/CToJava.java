package cc.quarkus.c_native.runtime;

import static cc.quarkus.c_native.api.CNative.*;

import cc.quarkus.c_native.api.NativeInterceptor;

/**
 *
 */
public final class CToJava implements NativeInterceptor {
    public object invoke(final Next next, final object... args) {
        final ptr<?> currentThread = VM.ni_current_thread;
        assert currentThread.isNonZero();
        VM.ni_current_thread = zero();
        // todo set Java current thread from pointer
        try {
            return next.proceed(args);
        } finally {
            // todo unset Java current thread
            // restore
            VM.ni_current_thread = currentThread;
        }
    }
}
