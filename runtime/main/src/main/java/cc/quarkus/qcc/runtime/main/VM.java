package cc.quarkus.qcc.runtime.main;

import static cc.quarkus.qcc.runtime.CNative.*;

import cc.quarkus.qcc.runtime.CNative;
import cc.quarkus.qcc.runtime.ThreadScoped;

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
    static CNative.ptr<?> _qcc_bound_thread;
}
