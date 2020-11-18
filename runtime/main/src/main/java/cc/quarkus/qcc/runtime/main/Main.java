package cc.quarkus.qcc.runtime.main;

import static cc.quarkus.qcc.runtime.CNative.*;
import static cc.quarkus.qcc.runtime.posix.PThread.*;

import cc.quarkus.qcc.runtime.Build;
import cc.quarkus.qcc.runtime.Detached;
import cc.quarkus.qcc.runtime.NotReachableException;

/**
 * Holds the native image main entry point.
 */
public final class Main {
    private Main() {
    }

    @export
    @Detached
    public static c_int main(c_int argc, ptr<c_char>[] argv) {

        // first set up VM
        // ...
        // next set up the initial thread
        // ...
        // now cause the initial thread to invoke main
        final String[] args = new String[argc.intValue()];
        for (int i = 1; i < argc.intValue(); i++) {
            args[i] = utf8zToJavaString(argv[i].cast());
        }
        String execName = utf8zToJavaString(argv[0].cast());

        if (Build.Target.isPosix()) {
            pthread_exit(zero());
        }
        // todo: windows
        throw new NotReachableException();
    }
}
