import static org.qbicc.runtime.CNative.*;

public class Synchronized {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) throws InterruptedException {
        /* Pattern for commented tests is: ^01234567#01234567#01234567#01234567#01234567# */
        for (TestThread.TestVariation var: TestThread.TestVariation.values()) {
            // TODO limit to two variations - will run out of memory without a GC
            if (var.equals(TestThread.TestVariation.METHOD_INST) || var.equals(TestThread.TestVariation.EMBEDDED)) {
                CountTest countTest = new CountTest();
                TestThread t1 = new TestThread(countTest, var);
                TestThread t2 = new TestThread(countTest, var);
                t1.start();
                t2.start();
                t1.join();
                t2.join();
                putchar('#');
            }
        }
    }
}

class TestThread extends Thread {
    CountTest countTest;
    TestVariation variation;
    int n = 4; // count per thread

    enum TestVariation {
        METHOD_INST,
        METHOD_STATIC,
        SEGMENT,
        REENTRANT,
        EMBEDDED
    };

    TestThread(CountTest countTest, TestVariation variation) {
        this.countTest = countTest;
        this.variation = variation;
    }

    public void run() {
        for (int i = 0; i < n; i++) {
            if (variation.equals(TestVariation.METHOD_INST)) {
                countTest.synchInstance();
            } else if (variation.equals(TestVariation.METHOD_STATIC)) {
                countTest.synchStatic();
            } else if (variation.equals(TestVariation.SEGMENT)) {
                countTest.segment();
            } else if (variation.equals(TestVariation.REENTRANT)) {
                countTest.reentrant();
            } else if (variation.equals(TestVariation.EMBEDDED)) {
                countTest.embedded();
            }
        }
    }
}

class CountTest {
    static int staticCount = 0;
    int count = 0;

    @extern
    public static native int putchar(int arg);

    private static void printInt(int n) {
        char[] numbers = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        boolean seenNonZero = false;
        int divsor = 1000000000;
        do {
            int i = n / divsor;
            if (!seenNonZero && i == 0) {
                // skip
            } else {
                seenNonZero = true;
                putchar(numbers[i]);
            }
            n %= divsor;
            divsor /= 10;
        } while (divsor != 0);
        if (!seenNonZero) {
            putchar(numbers[0]);
        }
    }

    /* synchronized instance method */
    public synchronized void synchInstance() {
        printInt(count++);
    }

    /* synchronized static method */
    public static synchronized void synchStatic() {
        printInt(staticCount++);
    }

    /* segment synchronized on instance */
    public void segment() {
        synchronized(this) {
            printInt(count++);
        }
    }

    /* re-enter synchronized block on same object */
    public void reentrant() {
        synchronized(this) {
            printInt(count);
            synchronized(this) {
                count++;
            }
        }
    }

    /* embed more than one synchronized block */
    public void embedded() {
        Object o = new Object();
        synchronized(this) {
            synchronized(o) {
                printInt(count++);
            }
        }
    }
}
