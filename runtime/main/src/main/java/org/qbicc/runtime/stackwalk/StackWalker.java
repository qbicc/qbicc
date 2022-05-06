package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.StackObject;
import org.qbicc.runtime.stdc.Stdint.uint64_t_ptr;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.llvm.LLVM.*;

import static org.qbicc.runtime.stdc.String.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

public final class StackWalker extends StackObject {
    unw_context_t context;
    unw_cursor_t cursor;
    int index = -1;
    boolean ready;

    public StackWalker() {
        unw_context_t_ptr context_ptr = addr_of(refToPtr(this).sel().context);
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
            """, "={x0},{x0},~{x1},~{memory}", ASM_FLAG_SIDE_EFFECT, context_ptr);
        } else {
            unw_getcontext(context_ptr);
        }
        unw_init_local(addr_of(refToPtr(this).sel().cursor), context_ptr);
    }

    public StackWalker(StackWalker orig) {
        memcpy(addr_of(refToPtr(this).sel().cursor).cast(), addr_of(refToPtr(orig).sel().cursor).cast(), sizeof(unw_cursor_t.class));
        index = orig.index;
        ready = orig.ready;
    }

    public boolean next() {
        if (unw_step(addr_of(refToPtr(this).sel().cursor)).isGt(zero())) {
            index++;
            return ready = true;
        }
        return ready = false;
    }

    public void_ptr getIp() {
        if (! ready) throw new IllegalStateException();
        unw_word_t ip = auto();
        unw_get_reg(addr_of(refToPtr(this).sel().cursor), UNW_REG_IP, addr_of(ip));
        return ip.cast();
    }

    public void_ptr getSp() {
        if (! ready) throw new IllegalStateException();
        unw_word_t sp = auto();
        unw_get_reg(addr_of(refToPtr(this).sel().cursor), UNW_REG_SP, addr_of(sp));
        return sp.cast();
    }

    /**
     * Overwrite the current cursor (iteration) position. This allows the stack walker to be rewound or to
     * skip over frames.
     *
     * @param ptr a pointer to the new cursor value (must not be {@code null})
     */
    public void setCursor(ptr<@c_const unw_cursor_t> ptr) {
        memcpy(addr_of(refToPtr(this).sel().cursor).cast(), ptr.cast(), sizeof(unw_cursor_t.class));
    }

    /**
     * Get a copy of the current cursor (iteration) position. The value can be later set in {@link #setCursor(ptr)}
     * to restore the current position.
     *
     * @param ptr a pointer to memory into which the copy should be stored (must not be {@code null})
     */
    public void getCursor(ptr<unw_cursor_t> ptr) {
        memcpy(ptr.cast(), addr_of(refToPtr(this).sel().cursor).cast(), sizeof(unw_cursor_t.class));
    }

    public int getIndex() {
        return index;
    }

    public void walk(StackFrameVisitor visitor, int limit) {
        for (int i = 0; i < limit && next(); i ++) {
            visitor.visitFrame(getIndex(), getIp().longValue(), getSp().longValue());
        }
    }

    public void walk(StackFrameVisitor visitor) {
        while (next()) {
            visitor.visitFrame(getIndex(), getIp().longValue(), getSp().longValue());
        }
    }

    public static void walkStack(StackFrameVisitor visitor) {
        new StackWalker().walk(visitor);
    }
}
