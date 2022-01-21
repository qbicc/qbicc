package org.qbicc.runtime.main;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.ThreadScoped;

/**
 * VM Utilities.
 */
public final class VM {

    /**
     * Internal holder for the pointer to the current thread.  Thread objects are not allowed to move in memory
     * after being constructed.
     * <p>
     * GC must take care to include this object in the root set of each thread.
     */
    @ThreadScoped
    @export
    @SuppressWarnings("unused")
    static Thread _qbicc_bound_thread;
}
