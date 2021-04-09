package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.Signal.*;
import static org.qbicc.runtime.stdc.Signal.*;

import java.util.function.BooleanSupplier;

import org.qbicc.runtime.Build;

@include("<ucontext.h>")
public class Ucontext {
    public static native c_int getcontext(ucontext_t_ptr ucp);
    // setcontext, makecontext, and swapcontext were removed in POSIX.1-2008

    public static final class ucontext_t extends object {
        public ucontext_t_ptr uc_link;
        public sigset_t uc_sigmask;
        public stack_t uc_stack;
        public mcontext_t uc_mcontext;
    }

    public static final class ucontext_t_ptr extends ptr<ucontext_t> {}
    public static final class const_ucontext_t_ptr extends ptr<@c_const ucontext_t> {}
    public static final class ucontext_t_ptr_ptr extends ptr<ucontext_t_ptr> {}
    public static final class const_ucontext_t_ptr_ptr extends ptr<const_ucontext_t_ptr> {}
    public static final class ucontext_t_ptr_const_ptr extends ptr<@c_const ucontext_t_ptr> {}
    public static final class const_ucontext_t_ptr_const_ptr extends ptr<@c_const const_ucontext_t_ptr> {}


    @incomplete(unless = Build.Target.IsAmd64.class)
    public static final class greg_t extends word {}

    @define("__USE_MISC")
    @define("__USE_GNU")
    public static final class mcontext_t extends object {

        @incomplete(unless = HasGRegs.class)
        // gregset_t == greg_t[NGREG] but we can probe the size rather than specifying
        public greg_t[] gregs;
    }

    @define("__USE_GNU")
    @incomplete(unless = HasGRegs.class)
    public static final c_int REG_PC = constant();

    public static final class HasGRegs implements BooleanSupplier {
        public boolean getAsBoolean() {
            return Build.Target.isLinux() && (Build.Target.isAmd64() || Build.Target.isI386());
        }
    }
}
