package org.qbicc.tests.snippets;

import static org.qbicc.runtime.CNative.*;

public class Synchronized {
    @extern
    public static native int putchar(int arg);

    public static void putbool(boolean val) {
        putchar(val ? 'T' : 'F');
    }

    public static void main(String[] args) throws InterruptedException {
        Object o1 = new Object();
        Object o2 = new Object();

        putbool(Thread.holdsLock(o1));
        putbool(Thread.holdsLock(o2));

        synchronized (o1) {
            putbool(Thread.holdsLock(o1));
            putbool(Thread.holdsLock(o2));
        }

        putbool(Thread.holdsLock(o1));
        putbool(Thread.holdsLock(o2));

        synchronized (o1) {
            synchronized (o2) {
                putbool(Thread.holdsLock(o1));
                putbool(Thread.holdsLock(o2));
            }
            putbool(Thread.holdsLock(o1));
            putbool(Thread.holdsLock(o2));
            synchronized (o1) {
                putbool(Thread.holdsLock(o1));
                putbool(Thread.holdsLock(o2));
            }
            putbool(Thread.holdsLock(o1));
            putbool(Thread.holdsLock(o2));
        }

        putbool(Thread.holdsLock(o1));
        putbool(Thread.holdsLock(o2));

        synchronized (o1) {
            putbool(Thread.holdsLock(o1));
            putbool(Thread.holdsLock(o2));
        }
    }
}
