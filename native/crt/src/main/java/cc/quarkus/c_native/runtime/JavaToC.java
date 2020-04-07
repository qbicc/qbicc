package cc.quarkus.c_native.runtime;

import static cc.quarkus.c_native.api.CNative.*;

import cc.quarkus.c_native.api.NativeInterceptor;

/**
 *
 */
public final class JavaToC implements NativeInterceptor {
    public object invoke(final Next next, final object... args) {
        assert VM.ni_current_thread.isNull();
        VM.ni_current_thread = zero(); // TODO: get java thread pointer from current thread instead of zero
        try {
            return next.proceed(args);
        } finally {
            // restore zero to avoid dirty pointers hanging around
            VM.ni_current_thread = zero();
        }
    }
}
