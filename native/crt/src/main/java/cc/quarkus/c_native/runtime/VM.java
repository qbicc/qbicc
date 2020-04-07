package cc.quarkus.c_native.runtime;

import static cc.quarkus.c_native.api.CNative.*;

import cc.quarkus.api.ThreadScoped;

/**
 * The overall VM state.
 */
public final class VM {


    @export
    @ThreadScoped
    static ptr<?> ni_current_thread;



    /**
     * The VM thread state.
     */
    public static final class Thread {

    }
}
