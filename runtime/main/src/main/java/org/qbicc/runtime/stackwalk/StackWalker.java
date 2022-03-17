package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.CNative;
import org.qbicc.runtime.stdc.Stdint.uint64_t_ptr;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.llvm.LLVM.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

public class StackWalker {
    public static void walkStack(StackFrameVisitor visitor) {
        unw_cursor_t_ptr cursor = alloca(CNative.sizeof(unw_cursor_t.class));
        unw_context_t_ptr uc = alloca(CNative.sizeof(unw_context_t.class));
        unw_word_t_ptr ip = alloca(CNative.sizeof(unw_word_t.class));
        unw_word_t_ptr sp = alloca(CNative.sizeof(unw_word_t.class));

        if (Build.Target.isAarch64() && ! Build.Target.isMacOs()) {
            // Expand the inline assembly that `unw_getcontext` corresponds to.
            // The layout of `unw_context_t` for aarch64 has not changed since 2017 when support was first introduced.
            asm(uint64_t_ptr.class, """
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
                mov x1, sp
                stp x1, x30, [$0, #248]
            """, "={x0},{x0},~{x1},~{memory}", ASM_FLAG_SIDE_EFFECT, uc);
        } else {
            unw_getcontext(uc);
        }
        unw_init_local(cursor, uc);
        int index = 0;
        while (unw_step(cursor).intValue() > 0) {
            unw_get_reg(cursor, UNW_REG_IP, ip);
            unw_get_reg(cursor, UNW_REG_SP, sp);

            visitor.visitFrame(index, ip.loadUnshared().longValue(), sp.loadUnshared().longValue());
            index += 1;
        }
    }
}
