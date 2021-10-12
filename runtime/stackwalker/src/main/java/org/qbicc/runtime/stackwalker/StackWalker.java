package org.qbicc.runtime.stackwalker;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.unwind.Context.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;
import static org.qbicc.runtime.unwind.LibUnwind.unw_get_reg;

public class StackWalker {
    public static boolean walkStack(StackFrameVisitor visitor) {
        unw_cursor_t_ptr cursor = alloca(sizeof(unw_cursor_t.class));
        ucontext_t_ptr uc = alloca(sizeof(ucontext_t.class));
        unw_word_t_ptr ip = alloca(sizeof(unw_word_t.class));
        unw_word_t_ptr sp = alloca(sizeof(unw_word_t.class));

        getcontext(uc);
        unw_context_t_ptr unw_context = uc.cast();
        unw_init_local(cursor, unw_context);
        while (unw_step(cursor).intValue() > 0) {
            unw_get_reg(cursor, UNW_REG_IP, ip);
            unw_get_reg(cursor, UNW_REG_IP, sp);
            if (!visitor.visitFrame(ip.deref().longValue(), sp.deref().longValue())) {
                return false;
            }
        }
        return true;
    }
}
