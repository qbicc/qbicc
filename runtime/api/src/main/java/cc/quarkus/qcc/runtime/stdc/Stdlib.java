package cc.quarkus.qcc.runtime.stdc;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

import cc.quarkus.qcc.runtime.NoReturn;
import cc.quarkus.qcc.runtime.Build.Target.IsGLibC;

/**
 *
 */
@include("<stdlib.h>")
@define(value = "_DEFAULT_SOURCE", when = IsGLibC.class)
public final class Stdlib {

    // heap

    public static native <T extends object> ptr<T> malloc(size_t size);

    public static <T extends object> ptr<T> malloc(Class<T> type) {
        return malloc(sizeof(type));
    }

    public static native <T extends object> ptr<T> calloc(size_t nMembers, size_t size);

    public static <T extends object> T[] callocArray(Class<T> type, int count) {
        return Stdlib.<T> calloc(sizeof(type), word(count)).asArray();
    }

    public static native <T extends object> ptr<T> realloc(ptr<T> ptr, size_t size);

    /**
     * Free the memory referenced by the given pointer.
     *
     * @param ptr a pointer containing the address of memory to free
     */
    public static native void free(ptr<?> ptr);

    // process exit

    @NoReturn
    public static native void abort();

    @NoReturn
    public static native void exit(c_int exitCode);

    public static native c_int atexit(ptr<function<Runnable>> function);

    // environment - thread unsafe wrt other env-related operations

    public static native ptr<c_char> getenv(ptr<@c_const c_char> name);

    public static native c_int setenv(ptr<@c_const c_char> name, ptr<@c_const c_char> value, c_int overwrite);

    public static native c_int unsetenv(ptr<@c_const c_char> name);

    public static native c_int clearenv();

    public static final c_int EXIT_SUCCESS = constant();
    public static final c_int EXIT_FAILURE = constant();
}
