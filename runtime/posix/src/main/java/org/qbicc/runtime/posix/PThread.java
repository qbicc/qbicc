package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Signal.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Time.*;

import org.qbicc.runtime.NoReturn;

/**
 *
 */
@include("<pthread.h>")
@lib("pthread")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class PThread {

    public static final c_int PTHREAD_MUTEX_RECURSIVE = constant();

    public static class pthread_t extends word {}

    public static final class pthread_t_ptr extends ptr<pthread_t> {}
    public static final class const_pthread_t_ptr extends ptr<@c_const pthread_t> {}
    public static final class pthread_t_ptr_ptr extends ptr<pthread_t_ptr> {}
    public static final class const_pthread_t_ptr_ptr extends ptr<const_pthread_t_ptr> {}
    public static final class pthread_t_ptr_const_ptr extends ptr<@c_const pthread_t_ptr> {}
    public static final class const_pthread_t_ptr_const_ptr extends ptr<@c_const const_pthread_t_ptr> {}

    public static class pthread_attr_t extends word {}

    public static final class pthread_attr_t_ptr extends ptr<pthread_attr_t> {}
    public static final class const_pthread_attr_t_ptr extends ptr<@c_const pthread_attr_t> {}
    public static final class pthread_attr_t_ptr_ptr extends ptr<pthread_attr_t_ptr> {}
    public static final class const_pthread_attr_t_ptr_ptr extends ptr<const_pthread_attr_t_ptr> {}
    public static final class pthread_attr_t_ptr_const_ptr extends ptr<@c_const pthread_attr_t_ptr> {}
    public static final class const_pthread_attr_t_ptr_const_ptr extends ptr<@c_const const_pthread_attr_t_ptr> {}

    public static class pthread_mutex_t extends word {}

    public static final class pthread_mutex_t_ptr extends ptr<pthread_mutex_t> {}
    public static final class const_pthread_mutex_t_ptr extends ptr<@c_const pthread_mutex_t> {}
    public static final class pthread_mutex_t_ptr_ptr extends ptr<pthread_mutex_t_ptr> {}
    public static final class const_pthread_mutex_t_ptr_ptr extends ptr<const_pthread_mutex_t_ptr> {}
    public static final class pthread_mutex_t_ptr_const_ptr extends ptr<@c_const pthread_mutex_t_ptr> {}
    public static final class const_pthread_mutex_t_ptr_const_ptr extends ptr<@c_const const_pthread_mutex_t_ptr> {}

    public static class pthread_mutexattr_t extends word {}

    public static final class pthread_mutexattr_t_ptr extends ptr<pthread_mutexattr_t> {}
    public static final class const_pthread_mutexattr_t_ptr extends ptr<@c_const pthread_mutexattr_t> {}
    public static final class pthread_mutexattr_t_ptr_ptr extends ptr<pthread_mutexattr_t_ptr> {}
    public static final class const_pthread_mutexattr_t_ptr_ptr extends ptr<const_pthread_mutexattr_t_ptr> {}
    public static final class pthread_mutexattr_t_ptr_const_ptr extends ptr<@c_const pthread_mutexattr_t_ptr> {}
    public static final class const_pthread_mutexattr_t_ptr_const_ptr extends ptr<@c_const const_pthread_mutexattr_t_ptr> {}

    public static class pthread_cond_t extends word {}

    public static final class pthread_cond_t_ptr extends ptr<pthread_cond_t> {}
    public static final class const_pthread_cond_t_ptr extends ptr<@c_const pthread_cond_t> {}
    public static final class pthread_cond_t_ptr_ptr extends ptr<pthread_cond_t_ptr> {}
    public static final class const_pthread_cond_t_ptr_ptr extends ptr<const_pthread_cond_t_ptr> {}
    public static final class pthread_cond_t_ptr_const_ptr extends ptr<@c_const pthread_cond_t_ptr> {}
    public static final class const_pthread_cond_t_ptr_const_ptr extends ptr<@c_const const_pthread_cond_t_ptr> {}

    public static class pthread_condattr_t extends word {}

    public static final class pthread_condattr_t_ptr extends ptr<pthread_condattr_t> {}
    public static final class const_pthread_condattr_t_ptr extends ptr<@c_const pthread_condattr_t> {}
    public static final class pthread_condattr_t_ptr_ptr extends ptr<pthread_condattr_t_ptr> {}
    public static final class const_pthread_condattr_t_ptr_ptr extends ptr<const_pthread_condattr_t_ptr> {}
    public static final class pthread_condattr_t_ptr_const_ptr extends ptr<@c_const pthread_condattr_t_ptr> {}
    public static final class const_pthread_condattr_t_ptr_const_ptr extends ptr<@c_const const_pthread_condattr_t_ptr> {}

    public static native c_int pthread_attr_init(pthread_attr_t_ptr attr);
    public static native c_int pthread_attr_destroy(pthread_attr_t_ptr attr);

    public static native c_int pthread_attr_getstack(const_pthread_attr_t_ptr attr, void_ptr_ptr stackAddr, size_t_ptr stackSize);
    public static native c_int pthread_attr_setstack(pthread_attr_t_ptr attr, void_ptr stackAddr, size_t stackSize);

    public static native c_int pthread_create(pthread_t_ptr thread, const_pthread_attr_t_ptr attr,
                                              void_ptr_unaryoperator_function_ptr start_routine, void_ptr arg);

    public static native pthread_t pthread_self();

    public static native c_int pthread_detach(pthread_t thread);

    @NoReturn
    public static native void pthread_exit(void_ptr arg);

    public static native c_int pthread_kill(pthread_t thread, c_int signal);

    public static native c_int pthread_sigmask(c_int how, const_sigset_t_ptr set, sigset_t_ptr oldSet);

    public static native c_int pthread_mutex_init(pthread_mutex_t_ptr mutex, const_pthread_mutexattr_t_ptr attr);
    public static native c_int pthread_mutex_lock(pthread_mutex_t_ptr mutex);
    public static native c_int pthread_mutex_unlock(pthread_mutex_t_ptr mutex);
    public static native c_int pthread_mutex_destroy(pthread_mutex_t_ptr mutex);

    public static native c_int pthread_mutexattr_init(pthread_mutexattr_t_ptr attr);
    public static native c_int pthread_mutexattr_settype(pthread_mutexattr_t_ptr attr, c_int type);
    public static native c_int pthread_mutexattr_destroy(pthread_mutexattr_t_ptr attr);

    public static native c_int pthread_cond_init(pthread_cond_t_ptr cond, pthread_condattr_t_ptr attr);
    public static native c_int pthread_cond_destroy(pthread_cond_t_ptr cond);
    public static native c_int pthread_cond_signal(pthread_cond_t_ptr cond);
    public static native c_int pthread_cond_broadcast(pthread_cond_t_ptr cond);
    public static native c_int pthread_cond_wait(pthread_cond_t_ptr cond, pthread_mutex_t_ptr mutex);
    public static native c_int pthread_cond_timedwait(pthread_cond_t_ptr cond, pthread_mutex_t_ptr mutex, const_struct_timespec_ptr abstime);

    public static native c_int pthread_condattr_init(pthread_condattr_t_ptr attr);
    public static native c_int pthread_condattr_destroy(pthread_condattr_t_ptr attr);
}
