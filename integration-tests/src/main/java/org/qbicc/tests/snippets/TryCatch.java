package org.qbicc.tests.snippets;

import static org.qbicc.runtime.CNative.*;

public class TryCatch {

    private static class DummyException extends Exception {
    }

    private static class SubException1 extends DummyException {
    }

    private static class SubException2 extends DummyException {
    }

    @extern
    public static native int putchar(int arg);

    public static void reportResult(int result) {
        if (result == 0) {
            putchar('P'); // pass
        } else {
            putchar('F'); // fail
        }
    }


    public static void throwException() throws DummyException {
        throw new DummyException();
    }

    public static int throwExceptionReturnNotVoid() throws DummyException {
        throw new DummyException();
    }

    public static void throwSubException(int type) throws DummyException {
        if (type == 1) {
            throw new SubException1();
        } else {
            throw new SubException2();
        }
    }

    public static void throwUncheckedException() {
        throw new ArithmeticException();
    }

    public static int test1() {
        try {
            throwException();
        } catch(DummyException e) {
            return 0;
        }
        return 1;
    }

    public static int test2() {
        try {
            throwException();
        } catch(DummyException e) {
            return 1;
        } finally {
            return 0;
        }
    }

    public static int test3() {
        try {
            throwSubException(2);
        } catch(SubException1 e) {
            return 1;
        } catch(SubException2 e) {
            return 0; // control should reach here
        } catch(DummyException e) {
            return 3;
        }
        return 4;
    }

    public int test4() {
        try {
            TryCatch.throwException();
        } catch(DummyException e) {
            return 0;
        }
        return 1;
    }

    public static void bar() throws Exception {
        throw new ArithmeticException();
    }

    public static int foo() throws Exception {
        try {
            bar();
        } catch(NullPointerException e) {
            return 2;
        }
        return 3;
    }

    public static int test5() {
        try {
            foo();
        } catch(Exception e) {
            return 0;
        }
        return 1;
    }

    public static int test6() {
        try {
            return throwExceptionReturnNotVoid();
        } catch (DummyException e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        int rc = test1();
        reportResult(rc);
        rc = test2();
        reportResult(rc);
        rc = test3();
        reportResult(rc);
        rc = new TryCatch().test4();
        reportResult(rc);
        rc = test5();
        reportResult(rc);
        /* test6() current results in crash.
        rc = test6();
        reportResult(rc);
         */
    }
}
