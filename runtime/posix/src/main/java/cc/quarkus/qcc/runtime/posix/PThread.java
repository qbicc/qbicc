package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Signal.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

import java.util.function.UnaryOperator;

import cc.quarkus.qcc.runtime.NoReturn;

/**
 *
 */
@include("<pthread.h>")
@lib("pthread")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class PThread {

    public static final c_int PTHREAD_MUTEX_RECURSIVE = constant();

    public static class pthread_t extends word {
    }

    public static class pthread_attr_t extends word {
    }

    public static class pthread_mutex_t extends word {
    }

    public static class pthread_mutexattr_t extends word {
    }

    public static native c_int pthread_attr_init(ptr<pthread_attr_t> attr);
    public static native c_int pthread_attr_destroy(ptr<pthread_attr_t> attr);

    public static native c_int pthread_attr_getstack(ptr<@c_const pthread_attr_t> attr, ptr<ptr<?>> stackAddr, ptr<size_t> stackSize);
    public static native c_int pthread_attr_setstack(ptr<pthread_attr_t> attr, ptr<?> stackAddr, size_t stackSize);

    public static native c_int pthread_create(ptr<pthread_t> thread, ptr<@c_const pthread_attr_t> attr,
            ptr<function<UnaryOperator<ptr<?>>>> start_routine, ptr<?> arg);

    public static native pthread_t pthread_self();

    public static native c_int pthread_detach(pthread_t thread);

    @NoReturn
    public static native void pthread_exit(ptr<?> arg);

    public static native c_int pthread_kill(pthread_t thread, c_int signal);

    public static native c_int pthread_sigmask(c_int how, ptr<@c_const sigset_t> set, ptr<sigset_t> oldSet);

    public static native c_int pthread_mutex_init(ptr<pthread_mutex_t> mutex, ptr<@c_const pthread_mutexattr_t> attr);
    public static native c_int pthread_mutex_lock(ptr<pthread_mutex_t> mutex);
    public static native c_int pthread_mutex_unlock(ptr<pthread_mutex_t> mutex);
    public static native c_int pthread_mutex_destroy(ptr<pthread_mutex_t> mutex);

    public static native c_int pthread_mutexattr_init(ptr<pthread_mutexattr_t> attr);
    public static native c_int pthread_mutexattr_settype(ptr<pthread_mutexattr_t> attr, c_int type);
    public static native c_int pthread_mutexattr_destroy(ptr<pthread_mutexattr_t> attr);
}
