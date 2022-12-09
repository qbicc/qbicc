package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

@include("<fenv.h>")
// todo: #pragma STDC FENV_ACCESS ON
public final class Fenv {
    public static final class fenv_t extends object {}
    public static final class fexcept_t extends word {}

    public static native c_int feclearexcept(c_int excepts);
    public static native c_int fetestexcept(c_int excepts);
    public static native c_int feraiseexcept(c_int excepts);

    public static native c_int fegetexceptflag(ptr<fexcept_t> flagp, c_int excepts);
    public static native c_int fesetexceptflag(ptr<@c_const fexcept_t> flagp, c_int excepts);

    public static native c_int fesetround(c_int round);
    public static native c_int fegetround();

    public static native c_int fegetenv(ptr<fenv_t> envp);
    public static native c_int fesetenv(ptr<@c_const fenv_t> envp);

    public static native c_int feholdexcept(ptr<fenv_t> envp);

    public static native c_int feupdateenv(ptr<@c_const fenv_t> envp);

    public static final c_int FE_ALL_EXCEPT = constant();
    public static final c_int FE_DIVBYZERO = constant();
    public static final c_int FE_INEXACT = constant();
    public static final c_int FE_INVALID = constant();
    public static final c_int FE_OVERFLOW = constant();
    public static final c_int FE_UNDERFLOW = constant();

    public static final c_int FE_DOWNWARD = constant();
    public static final c_int FE_TONEAREST = constant();
    public static final c_int FE_TOWARDZERO = constant();
    public static final c_int FE_UPWARD = constant();
}
