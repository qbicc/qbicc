package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.qcc.runtime.posix.Signal.*;
import static cc.quarkus.qcc.runtime.stdc.Signal.*;

import java.util.function.BooleanSupplier;

import cc.quarkus.qcc.runtime.api.Build;

@include("<ucontext.h>")
public class Ucontext {
    public static native c_int getcontext(ptr<ucontext_t> ucp);
    // setcontext, makecontext, and swapcontext were removed in POSIX.1-2008

    public static final class ucontext_t extends object {
        public ptr<ucontext_t> uc_link;
        public sigset_t uc_sigmask;
        public stack_t uc_stack;
        public mcontext_t uc_mcontext;
    }

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
