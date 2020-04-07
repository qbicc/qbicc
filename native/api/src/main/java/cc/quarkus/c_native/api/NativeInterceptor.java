package cc.quarkus.c_native.api;

import static cc.quarkus.c_native.api.CNative.*;

/**
 * Intercept a native call in or call out.  Used to freeze or thaw the VM state.
 */
public interface NativeInterceptor {
    object invoke(Next next, object... args);

    interface Next {
        object proceed(object... args);
    }
}
