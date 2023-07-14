package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

import java.util.function.Consumer;

/**
 *
 */
@include("<signal.h>")
public final class Signal {
    public static final class sigset_t extends object {}

    public static native ptr<function<sighandler_fn>> signal(c_int sigNum, ptr<function<sighandler_fn>> handler);

    public static native c_int raise(c_int signal);

    public static final ptr<function<Consumer<c_int>>> SIG_IGN = constant();
    public static final ptr<function<Consumer<c_int>>> SIG_DFL = constant();
    public static final ptr<function<Consumer<c_int>>> SIG_ERR = constant();

    public static final c_int SIGABRT = constant();
    public static final c_int SIGFPE = constant();
    public static final c_int SIGILL = constant();
    public static final c_int SIGINT = constant();
    public static final c_int SIGSEGV = constant();
    public static final c_int SIGTERM = constant();

    interface sighandler_fn {
        void handle_signal(c_int signal);
    }
}
