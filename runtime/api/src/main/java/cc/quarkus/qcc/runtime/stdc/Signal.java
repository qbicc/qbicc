package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import java.util.function.Consumer;

/**
 *
 */
@include("<signal.h>")
public final class Signal {
    public static final class sigset_t extends object {}

    public static final class sigset_t_ptr extends ptr<sigset_t> {}
    public static final class const_sigset_t_ptr extends ptr<@c_const sigset_t> {}
    public static final class sigset_t_ptr_ptr extends ptr<sigset_t_ptr> {}
    public static final class const_sigset_t_ptr_ptr extends ptr<const_sigset_t_ptr> {}
    public static final class sigset_t_ptr_const_ptr extends ptr<@c_const sigset_t_ptr> {}
    public static final class const_sigset_t_ptr_const_ptr extends ptr<@c_const const_sigset_t_ptr> {}

    public static native function_ptr<Consumer<c_int>> signal(c_int sigNum, function_ptr<Consumer<c_int>> handler);

    public static native c_int raise(c_int signal);

    public static final function_ptr<Consumer<c_int>> SIG_IGN = constant();
    public static final function_ptr<Consumer<c_int>> SIG_DFL = constant();
    public static final function_ptr<Consumer<c_int>> SIG_ERR = constant();

    public static final c_int SIGABRT = constant();
    public static final c_int SIGFPE = constant();
    public static final c_int SIGILL = constant();
    public static final c_int SIGINT = constant();
    public static final c_int SIGSEGV = constant();
    public static final c_int SIGTERM = constant();
}
