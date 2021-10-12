package org.qbicc.runtime.unwind;

import static org.qbicc.runtime.CNative.*;

@include("ucontext.h")
public final class Context {
    public static native c_int getcontext(ucontext_t_ptr ucp);
    public static native c_int setcontext(ucontext_t_ptr ucp);

    public static final class ucontext_t extends object {}

    public static final class ucontext_t_ptr extends ptr<ucontext_t> {}
    public static final class const_ucontext_t_ptr extends ptr<@c_const ucontext_t> {}
    public static final class ucontext_t_ptr_ptr extends ptr<ucontext_t_ptr> {}
    public static final class const_ucontext_t_ptr_ptr extends ptr<const_ucontext_t_ptr> {}
    public static final class ucontext_t_ptr_const_ptr extends ptr<@c_const ucontext_t_ptr> {}
    public static final class const_ucontext_t_ptr_const_ptr extends ptr<@c_const const_ucontext_t_ptr> {}

}
