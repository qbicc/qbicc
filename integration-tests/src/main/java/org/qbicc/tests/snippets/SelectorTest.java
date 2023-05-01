package org.qbicc.tests.snippets;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;

import static org.qbicc.runtime.CNative.*;

/**
 * A test of low-level native thread synchronization mechanisms.
 * CountDownLatch relies on native park/unpark support.
 * Selector relies on epoll/kqueue support.
 */
public class SelectorTest {
    private static final boolean VERBOSE = false;
    @extern
    public static native int putchar(int arg);

    public static void putbool(boolean val) {
        putchar(val ? 'T' : 'F');
    }

    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        new Thread(() -> {
            if (VERBOSE) System.out.println(Thread.currentThread() + " >> Count down");
            latch1.countDown();
            try {
                if (VERBOSE) System.out.println(Thread.currentThread() + " >> Start select");
                selector.select();
                if (VERBOSE) System.out.println(Thread.currentThread() + " >> End select");
                latch2.countDown();
            } catch (IOException e) {
                putbool(false);
                if (VERBOSE) e.printStackTrace();
            }
            putbool(true);
        }).start();

        if (VERBOSE) System.out.println(Thread.currentThread() +  " >> Start await");
        latch1.await();
        if (VERBOSE) System.out.println(Thread.currentThread() +  " >> End await");

        if (VERBOSE) System.out.println(Thread.currentThread() + " >> Wakeup thread");
        selector.wakeup();

        if (VERBOSE) System.out.println(Thread.currentThread() +  " >> Start await 2");
        latch2.await();
        if (VERBOSE) System.out.println(Thread.currentThread() +  " >> End await 2");

        //clean up
        selector.close();
        putbool(true);
    }
}
