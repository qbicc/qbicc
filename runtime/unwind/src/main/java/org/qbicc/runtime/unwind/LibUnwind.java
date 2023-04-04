package org.qbicc.runtime.unwind;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.Inline;
import org.qbicc.runtime.InlineCondition;
import org.qbicc.runtime.NoSafePoint;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.llvm.LLVM.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 * The libunwind library API @ <a href="https://www.nongnu.org/libunwind/docs.html">https://www.nongnu.org/libunwind/docs.html</a>
 */
@define("UNW_LOCAL_ONLY")
@include("<libunwind.h>")
@lib(value = "unwind", unless = { Build.Target.IsMacOs.class, Build.Target.IsWasm.class } )
public final class LibUnwind {
    private LibUnwind() {}

    @macro
    @name("unw_getcontext")
    private static native c_int unw_getcontext_actual(ptr<unw_context_t> context_ptr);

    private static final c_int _LIBUNWIND_VERSION = constant();

    @NoSafePoint
    @Inline(InlineCondition.ALWAYS)
    public static c_int unw_getcontext(ptr<unw_context_t> context_ptr) {
        if (Build.Target.isAarch64() && ! Build.Target.isMacOs() && Build.Target.isLlvm() && ! defined(_LIBUNWIND_VERSION)) {
            // The buggy unw_getcontext is present!
            // Expand the inline assembly that `unw_getcontext` corresponds to.
            // Workaround for https://github.com/libunwind/libunwind/issues/341
            // Mac OS X uses the LLVM implementation.
            // TODO: FP registers (needed for Loom context switching)
            asm(c_void.class, """
                ${:comment} Store the 31 GPRs in slots 0-30
                ${:comment} (We know x0 already is the base but store it anyway)
                stp x0, x1, [$0, #0]
                stp x2, x3, [$0, #16]
                stp x4, x5, [$0, #32]
                stp x6, x7, [$0, #48]
                stp x8, x9, [$0, #64]
                stp x10, x11, [$0, #80]
                stp x12, x13, [$0, #96]
                stp x14, x15, [$0, #112]
                stp x16, x17, [$0, #128]
                stp x18, x19, [$0, #144]
                stp x20, x21, [$0, #160]
                stp x22, x23, [$0, #176]
                stp x24, x25, [$0, #192]
                stp x26, x27, [$0, #208]
                stp x28, x29, [$0, #224]
                str x30, [$0, #240]
                ${:comment} Store SP in the 31 slot
                mov x1, sp
                str x1, [$0, #248]
                ${:comment} Store the PC in the 32 slot
                adr x1, ${:private}ret${:uid}
                str x1, [$0, #256]
                mrs x1, NZCV
                ${:comment} Store the flags in the 33 slot
                str x1, [$0, #264]
                ${:private}ret${:uid}:
            """, "{x0},~{x1},~{memory}", ASM_FLAG_SIDE_EFFECT, addr_of(deref(context_ptr).uc_mcontext.regs));
            // always successful
            return zero();
        } else {
            return unw_getcontext_actual(context_ptr);
        }
    }

    @macro
    public static native c_int unw_init_local(ptr<unw_cursor_t> cursor, ptr<unw_context_t> context_ptr);
    @macro
    public static native c_int unw_step(ptr<unw_cursor_t> cursor);
    @macro
    public static native c_int unw_get_reg(ptr<unw_cursor_t> cursor, unw_regnum_t reg, ptr<unw_word_t> output);
    @macro
    public static native c_int unw_set_reg(ptr<unw_cursor_t> cursor, unw_regnum_t reg, unw_word_t value);
    @macro
    public static native c_int unw_resume(ptr<unw_cursor_t> cursor);
    @macro
    public static native c_int unw_get_proc_info(ptr<unw_cursor_t> cursor, ptr<unw_proc_info_t> info);
    @macro
    public static native c_int unw_is_signal_frame(ptr<unw_cursor_t> cursor);

    public static final class unw_context_t extends struct {
        @incomplete(unless = Build.Target.IsAarch64.class, when = Build.Target.IsMacOs.class)
        public struct_unw_sigcontext uc_mcontext;

    }
    public static final class struct_unw_sigcontext extends struct {
        // this is just the first one; it is an array of length 31 followed by several discrete uint64_t fields, but this is simpler
        @incomplete(unless = Build.Target.IsAarch64.class, when = Build.Target.IsMacOs.class)
        public uint64_t regs;
    }
    public static final class unw_cursor_t extends object {}
    public static final class unw_addr_space_t extends object {}
    public static final class unw_word_t extends word {}
    public static final class unw_regnum_t extends word {}
    public static final class unw_proc_info_t extends struct {
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
