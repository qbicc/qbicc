package cc.quarkus.c_native.posix;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.c_native.posix.SysTypes.*;
import static cc.quarkus.qcc.runtime.stdc.Signal.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;
import static cc.quarkus.qcc.runtime.stdc.Time.*;

import java.util.function.Consumer;

import cc.quarkus.qcc.runtime.api.Build;

/**
 *
 * @see cc.quarkus.qcc.runtime.stdc.Signal
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

    public static final class stack_t extends object {
        ptr<?> ss_sp;
        c_int ss_flags;
        size_t ss_size;
    }

    // si_code values
    public static final c_int ILL_ILLOPC = constant();
    public static final c_int ILL_ILLOPN = constant();
    public static final c_int ILL_ILLADR = constant();
    public static final c_int ILL_ILLTRP = constant();
    public static final c_int ILL_PRVOPC = constant();
    public static final c_int ILL_COPROC = constant();
    public static final c_int ILL_BADSTK = constant();

    public static final c_int FPE_INTDIV = constant();
    public static final c_int FPE_INTOVF = constant();
    public static final c_int FPE_FLTDIV = constant();
    public static final c_int FPE_FLTOVF = constant();
    public static final c_int FPE_FLTUND = constant();
    public static final c_int FPE_FLTRES = constant();
    public static final c_int FPE_FLTINV = constant();
    public static final c_int FPE_FLTSUB = constant();

    public static final c_int SEGV_MAPERR = constant();
    public static final c_int SEGV_ACCERR = constant();

    public static final c_int BUS_ADRALN = constant();
    public static final c_int BUS_ADRERR = constant();
    public static final c_int BUS_OBJERR = constant();

    public static final c_int TRAP_BRKPT = constant();
    public static final c_int TRAP_TRACE = constant();

    public static final c_int CLD_EXITED = constant();
    public static final c_int CLD_KILLED = constant();
    public static final c_int CLD_DUMPED = constant();
    public static final c_int CLD_TRAPPED = constant();
    public static final c_int CLD_STOPPED = constant();
    public static final c_int CLD_CONTINUED = constant();

    public static final c_int POLL_IN = constant();
    public static final c_int POLL_OUT = constant();
    public static final c_int POLL_MSG = constant();
    public static final c_int POLL_ERR = constant();
    public static final c_int POLL_PRI = constant();
    public static final c_int POLL_HUP = constant();

    public static final c_int SI_USER = constant();
    public static final c_int SI_QUEUE = constant();
    public static final c_int SI_TIMER = constant();
    public static final c_int SI_ASYNCIO = constant();
    public static final c_int SI_MESGQ = constant();

    @incomplete(unless = Build.Target.IsLinux.class)
    public static final c_int SI_KERNEL = constant();

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
