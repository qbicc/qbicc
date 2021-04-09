package org.qbicc.runtime.main;

import org.qbicc.runtime.posix.PThread.pthread_mutex_t_ptr;

/* object wrapper for native objectmonitor mutex. */
public class NativeObjectMonitor {
    private final pthread_mutex_t_ptr pthread_mutex;

    NativeObjectMonitor(pthread_mutex_t_ptr pthread_mutex) {
        this.pthread_mutex = pthread_mutex;
    }

    pthread_mutex_t_ptr getPthreadMutex() {
        return pthread_mutex;
    }
}
