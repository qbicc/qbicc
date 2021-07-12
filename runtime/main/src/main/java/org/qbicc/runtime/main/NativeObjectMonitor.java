package org.qbicc.runtime.main;

import org.qbicc.runtime.posix.PThread.pthread_mutex_t_ptr;

/* object wrapper for native objectmonitor mutex. */
// TODO move this to be an inner class
public class NativeObjectMonitor {
    /*private final */pthread_mutex_t_ptr nomPthreadMutex;

    public static void forceClinit() {}

    NativeObjectMonitor(pthread_mutex_t_ptr nomPthreadMutex) {
        this.nomPthreadMutex = nomPthreadMutex;
    }

    pthread_mutex_t_ptr getPthreadMutex() {
        return nomPthreadMutex;
    }
}
