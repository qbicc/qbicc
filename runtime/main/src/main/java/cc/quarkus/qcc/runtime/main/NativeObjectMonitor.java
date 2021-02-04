package cc.quarkus.qcc.runtime.main;

import cc.quarkus.qcc.runtime.CNative.ptr;
import cc.quarkus.qcc.runtime.posix.PThread.pthread_mutex_t;

/* object wrapper for native objectmonitor mutex. */
public class NativeObjectMonitor {
    private final ptr<pthread_mutex_t> pthread_mutex;

    NativeObjectMonitor(ptr<pthread_mutex_t> pthread_mutex) {
        this.pthread_mutex = pthread_mutex;
    }

    ptr<pthread_mutex_t> getPthreadMutex() {
        return pthread_mutex;
    }
}
