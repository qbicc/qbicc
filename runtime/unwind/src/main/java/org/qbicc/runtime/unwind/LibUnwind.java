package org.qbicc.runtime.unwind;

import org.qbicc.runtime.Build;

import static org.qbicc.runtime.CNative.*;

/**
 * The libunwind library API @ <a href="https://www.nongnu.org/libunwind/docs.html">https://www.nongnu.org/libunwind/docs.html</a>
 */
@define("UNW_LOCAL_ONLY")
@include("<libunwind.h>")
@lib(value = "unwind", unless = { Build.Target.IsMacOs.class, Build.Target.IsWasm.class } )
public final class LibUnwind {
    private LibUnwind() {}

    @macro
    public static native c_int unw_getcontext(unw_context_t_ptr context_ptr);
    @macro
    public static native c_int unw_init_local(unw_cursor_t_ptr cursor, unw_context_t_ptr context_ptr);
    @macro
    public static native c_int unw_step(unw_cursor_t_ptr cursor);
    @macro
    public static native c_int unw_get_reg(unw_cursor_t_ptr cursor, unw_regnum_t reg, unw_word_t_ptr output);
    @macro
    public static native c_int unw_set_reg(unw_cursor_t_ptr cursor, unw_regnum_t reg, unw_word_t value);
    @macro
    public static native c_int unw_resume(unw_cursor_t_ptr cursor);
    @macro
    public static native c_int unw_get_proc_info(unw_cursor_t_ptr cursor, unw_proc_info_t_ptr info);
    @macro
    public static native c_int unw_is_signal_frame(unw_cursor_t_ptr cursor);

    public static final class unw_context_t extends object {}

    public static final class unw_context_t_ptr extends ptr<unw_context_t> {}
    public static final class const_unw_context_t_ptr extends ptr<@c_const unw_context_t> {}
    public static final class unw_context_t_ptr_ptr extends ptr<unw_context_t_ptr> {}
    public static final class const_unw_context_t_ptr_ptr extends ptr<const_unw_context_t_ptr> {}
    public static final class unw_context_t_ptr_const_ptr extends ptr<@c_const unw_context_t_ptr> {}
    public static final class const_unw_context_t_ptr_const_ptr extends ptr<@c_const const_unw_context_t_ptr> {}

    public static final class unw_cursor_t extends object {}

    public static final class unw_cursor_t_ptr extends ptr<unw_cursor_t> {}
    public static final class const_unw_cursor_t_ptr extends ptr<@c_const unw_cursor_t> {}
    public static final class unw_cursor_t_ptr_ptr extends ptr<unw_cursor_t_ptr> {}
    public static final class const_unw_cursor_t_ptr_ptr extends ptr<const_unw_cursor_t_ptr> {}
    public static final class unw_cursor_t_ptr_const_ptr extends ptr<@c_const unw_cursor_t_ptr> {}
    public static final class const_unw_cursor_t_ptr_const_ptr extends ptr<@c_const const_unw_cursor_t_ptr> {}

    public static final class unw_addr_space_t extends object {}
    public static final class unw_word_t extends word {}

    public static final class unw_word_t_ptr extends ptr<unw_word_t> {}
    public static final class const_unw_word_t_ptr extends ptr<@c_const unw_word_t> {}
    public static final class unw_word_t_ptr_ptr extends ptr<unw_word_t_ptr> {}
    public static final class const_unw_word_t_ptr_ptr extends ptr<const_unw_word_t_ptr> {}
    public static final class unw_word_t_ptr_const_ptr extends ptr<@c_const unw_word_t_ptr> {}
    public static final class const_unw_word_t_ptr_const_ptr extends ptr<@c_const const_unw_word_t_ptr> {}

    public static final class unw_regnum_t extends word {}

    public static final class unw_proc_info_t extends object {
        public unw_word_t start_ip;
        public unw_word_t end_ip;
        public unw_word_t lsda;
        public unw_word_t handler;
        public unw_word_t gp;
        public unw_word_t flags;
        public c_int format;
        public c_int unwind_info_size;
        public void_ptr unwind_info;
    }

    public static final class unw_proc_info_t_ptr extends ptr<unw_proc_info_t> {}
    public static final class const_unw_proc_info_t_ptr extends ptr<@c_const unw_proc_info_t> {}
    public static final class unw_proc_info_t_ptr_ptr extends ptr<unw_proc_info_t_ptr> {}
    public static final class const_unw_proc_info_t_ptr_ptr extends ptr<const_unw_proc_info_t_ptr> {}
    public static final class unw_proc_info_t_ptr_const_ptr extends ptr<@c_const unw_proc_info_t_ptr> {}
    public static final class const_unw_proc_info_t_ptr_const_ptr extends ptr<@c_const const_unw_proc_info_t_ptr> {}

    public static final unw_regnum_t UNW_REG_IP = constant();
    public static final unw_regnum_t UNW_REG_SP = constant();

    public static final c_int UNW_ESUCCESS = constant();
    public static final c_int UNW_EUNSPEC = constant();
    public static final c_int UNW_ENOMEM = constant();
    public static final c_int UNW_EBADREG = constant();
    public static final c_int UNW_EREADONLYREG = constant();
    public static final c_int UNW_ESTOPUNWIND = constant();
    public static final c_int UNW_EINVALIDIP = constant();
    public static final c_int UNW_EBADFRAME = constant();
    public static final c_int UNW_EINVAL = constant();
    public static final c_int UNW_EBADVERSION = constant();

    public static final c_int UNW_INFO_FORMAT_DYNAMIC = constant();
    public static final c_int UNW_INFO_FORMAT_TABLE = constant();
}
