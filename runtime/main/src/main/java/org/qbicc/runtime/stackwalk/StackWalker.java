package org.qbicc.runtime.stackwalk;

import static org.qbicc.runtime.CNative.*;

import static org.qbicc.runtime.stackwalk.CallSiteTable.*;
import static org.qbicc.runtime.stdc.String.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

import org.qbicc.runtime.Build;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.StackObject;

public final class StackWalker extends StackObject {
    unw_context_t context;
    unw_cursor_t cursor;
    int index = -1;
    boolean ready;

    @NoSafePoint
    @NoThrow
    public StackWalker() {
        ptr<unw_context_t> context_ptr = addr_of(deref(refToPtr(this)).context);
        unw_getcontext(context_ptr);
        unw_init_local(addr_of(deref(refToPtr(this)).cursor), context_ptr);
    }

    @NoSafePoint
    @NoThrow
    public StackWalker(ptr<unw_context_t> context_ptr) {
        memcpy(addr_of(deref(refToPtr(this)).context).cast(), context_ptr.cast(), sizeof(unw_context_t.class));
        unw_init_local(addr_of(deref(refToPtr(this)).cursor), context_ptr);
    }

    @SuppressWarnings("CopyConstructorMissesField")
    @NoSafePoint
    @NoThrow
    public StackWalker(StackWalker orig) {
        memcpy(addr_of(deref(refToPtr(this)).context).cast(), addr_of(deref(refToPtr(orig)).context).cast(), sizeof(unw_context_t.class));
        memcpy(addr_of(deref(refToPtr(this)).cursor).cast(), addr_of(deref(refToPtr(orig)).cursor).cast(), sizeof(unw_cursor_t.class));
        index = orig.index;
        ready = orig.ready;
    }

    @NoSafePoint
    @NoThrow
    public boolean next() {
        if (unw_step(addr_of(deref(refToPtr(this)).cursor)).isGt(zero())) {
            index++;
            return ready = true;
        }
        return ready = false;
    }

    @NoSafePoint
    @NoThrow
    public void_ptr getIp() {
        if (! ready) return zero();
        unw_word_t ip = auto();
        unw_get_reg(addr_of(deref(refToPtr(this)).cursor), UNW_REG_IP, addr_of(ip));
        if (Build.Target.isAarch64() && Build.Target.isMacOs()) {
            // This is the "right" thing to do, but we don't compile our code enabling pauth.
            // asm(unw_word_t.class, "xpaci $0", "=r,r", ASM_FLAG_SIDE_EFFECT, ip);
            ip = word(ip.longValue() & 0x00000FFFFFFFFFFFL);
        }
        return ip.cast();
    }

    @NoSafePoint
    @NoThrow
    public void_ptr getSp() {
        if (! ready) return zero();
        unw_word_t sp = auto();
        unw_get_reg(addr_of(deref(refToPtr(this)).cursor), UNW_REG_SP, addr_of(sp));
        return sp.cast();
    }

    /**
     * Overwrite the current cursor (iteration) position. This allows the stack walker to be rewound or to
     * skip over frames.
     *
     * @param ptr a pointer to the new cursor value (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    public void setCursor(ptr<@c_const unw_cursor_t> ptr) {
        memcpy(addr_of(deref(refToPtr(this)).cursor).cast(), ptr.cast(), sizeof(unw_cursor_t.class));
    }

    /**
     * Get a copy of the current cursor (iteration) position. The value can be later set in {@link #setCursor(ptr)}
     * to restore the current position.
     *
     * @param ptr a pointer to memory into which the copy should be stored (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    public void getCursor(ptr<unw_cursor_t> ptr) {
        memcpy(ptr.cast(), addr_of(deref(refToPtr(this)).cursor).cast(), sizeof(unw_cursor_t.class));
    }

    @NoSafePoint
    @NoThrow
    public int getIndex() {
        return index;
    }

    /**
     * Get a pointer to the call site represented by this iterator step.
     *
     * @return the call site pointer, or {@code null} if the call site is not known
     */
    @NoSafePoint
    @NoThrow
    public ptr<struct_call_site> getCallSite() {
        return findInsnTableEntry(getIp());
    }
}
