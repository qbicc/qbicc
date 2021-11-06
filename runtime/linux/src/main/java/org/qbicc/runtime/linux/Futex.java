package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.linux.SysSyscall.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;

/**
 * Support for {@code futex(2)} on Linux.  See the manpage for more information.
 */
@include("<linux/futex.h>")
public class Futex {

    private static c_long futex(uint32_t_ptr uaddr, c_int futex_op, uint32_t val, const_struct_timespec_ptr timeout, uint32_t_ptr uaddr2, uint32_t val3) {
        // no glibc wrapper!
        return syscall(SYS_futex, uaddr, futex_op, val, timeout, uaddr2, val3);
    }

    /**
     * Wait until the value at given address is equal to {@code val}.  Returns when woken (may be spurious).  Spurious
     * wakeups may return {@code false} and set {@code errno} to {@code EAGAIN}, or may return {@code true}.  If the
     * value at the test memory location already does not equal {@code val}, a wakeup will result.
     *
     * @param uaddr the address of the memory to test
     * @param val the value to observe
     * @param timeout the relative amount of time to wait
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    public static boolean futex_wait(uint32_t_ptr uaddr, uint32_t val, const_struct_timespec_ptr timeout) {
        return futex(uaddr, word(FUTEX_WAIT.intValue() | FUTEX_PRIVATE_FLAG.intValue() | FUTEX_CLOCK_REALTIME.intValue()), val, timeout, zero(), zero()).longValue() != -1;
    }

    /**
     * Wait until the value at given address is equal to {@code val}.  Returns when woken (may be spurious).  Spurious
     * wakeups may return {@code false} and set {@code errno} to {@code EAGAIN}, or may return {@code true}.  If the
     * value at the test memory location already does not equal {@code val}, a wakeup will result.
     *
     * @param uaddr the address of the memory to test
     * @param val the value to observe
     * @param timeout the absolute time to wake up
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    public static boolean futex_wait_absolute(uint32_t_ptr uaddr, uint32_t val, const_struct_timespec_ptr timeout) {
        return futex(uaddr, word(FUTEX_WAIT_BITSET.intValue() | FUTEX_PRIVATE_FLAG.intValue() | FUTEX_CLOCK_REALTIME.intValue()), val, timeout, zero(), FUTEX_BITSET_MATCH_ANY).longValue() != -1;
    }

    /**
     * Wake a single waiter after updating the wait value.
     *
     * @param uaddr the address of the memory being waited on
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    public static boolean futex_wake_single(uint32_t_ptr uaddr) {
        return futex(uaddr, word(FUTEX_WAKE.intValue() | FUTEX_PRIVATE_FLAG.intValue()), word(1), zero(), zero(), zero()).longValue() != -1;
    }

    /**
     * Wake all waiters after updating the wait value.
     *
     * @param uaddr the address of the memory being waited on
     * @return {@code true} on success, or {@code false} on error (in {@code errno})
     */
    public static boolean futex_wake_all(uint32_t_ptr uaddr) {
        return futex(uaddr, word(FUTEX_WAKE.intValue() | FUTEX_PRIVATE_FLAG.intValue()), word(Integer.MAX_VALUE), zero(), zero(), zero()).longValue() != -1;
    }

    public static final c_int FUTEX_PRIVATE_FLAG = constant();
    public static final c_int FUTEX_CLOCK_REALTIME = constant();

    public static final c_int FUTEX_WAIT = constant();
    public static final c_int FUTEX_WAIT_BITSET = constant();
    public static final c_int FUTEX_WAKE = constant();

    public static final uint32_t FUTEX_BITSET_MATCH_ANY = constant();
}
