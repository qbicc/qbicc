package cc.quarkus.qcc.runtime.main;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.PThread.pthread_exit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qcc.runtime.Detached;
import cc.quarkus.qcc.runtime.NotReachableException;

/**
 * Holds the native image main entry point.
 */
public final class Main {

    /* map Java object to native mutex for object monitor bytecodes. */
    static final ConcurrentMap<Object, NativeObjectMonitor> objectMonitorNatives = new ConcurrentHashMap<>();

    private Main() {
    }

    /**
     * This is a stub that gets redirected to the real main method by the main method plugin.
     *
     * @param args the arguments to pass to the main program
     */
    static native void userMain(String[] args);

    static native ThreadGroup createSystemThreadGroup();

    @export
    @Detached
    public static c_int main(c_int argc, char_ptr[] argv) {

        // first set up VM
        // ...
        // next set up the initial thread
        attachNewThread("main", createSystemThreadGroup());
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
