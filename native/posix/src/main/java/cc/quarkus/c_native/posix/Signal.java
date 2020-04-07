package cc.quarkus.c_native.posix;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.posix.SysTypes.*;
import static cc.quarkus.c_native.stdc.Signal.*;
import static cc.quarkus.c_native.stdc.Time.*;

import java.util.function.Consumer;

import cc.quarkus.c_native.api.Build;

/**
 *
 * @see cc.quarkus.c_native.stdc.Signal
 */
@include("<signal.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public final class Signal {
    private Signal() {
    }

    public static native c_int kill(pid_t pid, c_int sig);

    public static native c_int sigpending(ptr<sigset_t> set);

    public static native c_int sigaction(c_int sigNum, ptr<@c_const struct_sigaction> act, ptr<struct_sigaction> oldAct);

    public interface SignalAction {
        void handle(c_int sigNum, ptr<siginfo_t> sigInfo, ptr<?> data);
    }

    public static final class struct_sigaction extends object {
        public ptr<function<Consumer<c_int>>> sa_handler;
        public ptr<function<SignalAction>> sa_sigaction;
        public sigset_t sa_mask;
        public c_int sa_flags;
        public ptr<function<Runnable>> sa_restorer;
    }

    public static final class sigval_t extends object {
        public c_int sival_int;
        public ptr<?> sival_ptr;
    }

    public static final class siginfo_t extends object {
        public c_int si_signo;
        public c_int si_code;
        public c_int si_errno;
        public pid_t si_pid;
        public uid_t si_uid;
        public ptr<?> si_addr;
        public c_int si_status;
        public c_long si_band;
        public sigval_t si_value;

        @incomplete(unless = Build.Target.IsLinux.class)
        public clock_t si_utime;
        @incomplete(unless = Build.Target.IsLinux.class)
        public clock_t si_stime;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_int si_int;
        @incomplete(unless = Build.Target.IsLinux.class)
        public ptr<?> si_ptr;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_int si_overrun;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_int si_timerid;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_int si_fd;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_short si_addr_lsb;
        @incomplete(unless = Build.Target.IsLinux.class)
        public ptr<?> si_lower;
        @incomplete(unless = Build.Target.IsLinux.class)
        public ptr<?> si_upper;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_int si_pkey;
        @incomplete(unless = Build.Target.IsLinux.class)
        public ptr<?> si_call_addr;
        @incomplete(unless = Build.Target.IsLinux.class)
        public c_int si_syscall;
        @incomplete(unless = Build.Target.IsLinux.class)
        public unsigned_int si_arch;
    }

    // POSIX 1990
    // SIGABRT is defined by stdc
    public static final c_int SIGALRM = constant();
    public static final c_int SIGCHLD = constant();
    public static final c_int SIGCONT = constant();
    // SIGFPE is defined by stdc
    public static final c_int SIGHUP = constant();
    // SIGILL is defined by stdc
    // SIGINT is defined by stdc
    public static final c_int SIGKILL = constant();
    public static final c_int SIGPIPE = constant();
    public static final c_int SIGQUIT = constant();
    // SIGSEGV is defined by stdc
    public static final c_int SIGSTOP = constant();
    public static final c_int SIGTSTP = constant();
    // SIGTERM is defined by stdc
    public static final c_int SIGTTIN = constant();
    public static final c_int SIGTTOU = constant();
    public static final c_int SIGUSR1 = constant();
    public static final c_int SIGUSR2 = constant();

    // POSIX 2001
    public static final c_int SIGBUS = constant();
    public static final c_int SIGPOLL = constant();
    public static final c_int SIGPROF = constant();
    public static final c_int SIGSYS = constant();
    public static final c_int SIGTRAP = constant();
    public static final c_int SIGURG = constant();
    public static final c_int SIGVTALRM = constant();
    public static final c_int SIGXCPU = constant();
    public static final c_int SIGXFSZ = constant();

    // Nonstandard but widely supported
    public static final c_int SIGEMT = constant();
    public static final c_int SIGIO = constant();
    public static final c_int SIGPWR = constant();
    public static final c_int SIGWINCH = constant();
}
