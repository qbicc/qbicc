package cc.quarkus.qcc.runtime.main;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.PThread.*;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qcc.runtime.Detached;
import cc.quarkus.qcc.runtime.NotReachableException;
import cc.quarkus.qcc.runtime.ThreadScoped;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds the native image main entry point.
 */
public final class Main {

    /* map Java object to native mutex for object monitor bytecodes. */
    static final ConcurrentMap<Object, NativeObjectMonitor> objectMonitorNatives = new ConcurrentHashMap<>();

    /**
     * Internal holder for the pointer to the current thread.  Thread objects are not allowed to move in memory
     * after being constructed.
     * <p>
     * GC must take care to include this object in the root set of each thread.
     */
    @ThreadScoped
    @export
    @SuppressWarnings("unused")
    static ptr<?> _qcc_bound_thread;

    private Main() {
    }

    /**
     * This is a stub that gets redirected to the real main method by the main method plugin.
     *
     * @param args the arguments to pass to the main program
     */
    static native void userMain(String[] args);

    @export
    @Detached
    public static c_int main(c_int argc, ptr<c_char>[] argv) {

        // first set up VM
        // ...
        // next set up the initial thread
        attachNewThread("main");
        // now cause the initial thread to invoke main
        final String[] args = new String[argc.intValue()];
        for (int i = 1; i < argc.intValue(); i++) {
            args[i] = utf8zToJavaString(argv[i].cast());
        }
        String execName = utf8zToJavaString(argv[0].cast());
        userMain(args);
        if (Build.Target.isPosix()) {
            pthread_exit(zero());
        }
        // todo: windows
        throw new NotReachableException();
    }
}
