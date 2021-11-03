package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.CNative;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

public class StackWalker {
    public static void walkStack(StackFrameVisitor visitor) {
        unw_cursor_t_ptr cursor = alloca(CNative.sizeof(unw_cursor_t.class));
        unw_context_t_ptr uc = alloca(CNative.sizeof(unw_context_t.class));
        unw_word_t_ptr ip = alloca(CNative.sizeof(unw_word_t.class));
        unw_word_t_ptr sp = alloca(CNative.sizeof(unw_word_t.class));

        unw_getcontext(uc);
        unw_init_local(cursor, uc);
        int index = 0;
        while (unw_step(cursor).intValue() > 0) {
            unw_get_reg(cursor, UNW_REG_IP, ip);
            unw_get_reg(cursor, UNW_REG_IP, sp);

            visitor.visitFrame(index, ip.deref().longValue(), sp.deref().longValue());
            index += 1;
        }
    }
}
