package org.qbicc.tests.snippets;

import org.qbicc.runtime.CNative;

/**
 * Check that basic runtime checks like null pointer, array bounds, divide by zero
 * are being generated as expected.
 */
public class RuntimeChecks {
    static class A {
        int a;
        A (int x) {
            this.a = x;
        }
        int div(int b) {
            return b / a;
        }
    }

    @CNative.extern
    static native int putchar(int arg);

    static void putbool(boolean val) {
        putchar(val ? 'T' : 'F');
    }

    public static int testField(A a) {
        return a.a;
    }

    public static int testMethod(A a, int x) {
        return a.div(x);
    }

    public static int testArray(int [] xs) {
        return xs[0] + xs[1];
    }

    public static void main(String[] args) {
        A good = new A(10);
        A bad = new A(0);
        putbool(testField(good) == 10);
        try {
            testField(null);
            putbool(false);
        } catch (NullPointerException e) {
            putbool(true);
        }
        putbool(testMethod(good, 100) == 10);
        try {
            testMethod(null, 100);
            putbool(false);
        } catch (NullPointerException e) {
            putbool(true);
        }
        try {
            testMethod(bad, 100);
            putbool(false);
        } catch (ArithmeticException e) {
            putbool(true);
        }
        putbool(testArray(new int[]{ 10, 32}) == 42);
        try {
            testArray(null);
            putbool(false);
        } catch (NullPointerException e) {
            putbool(true);
        }
        try {
            testArray(new int[]{10});
            putbool(false);
        } catch (ArrayIndexOutOfBoundsException e) {
            putbool(true);
        }
    }
}
