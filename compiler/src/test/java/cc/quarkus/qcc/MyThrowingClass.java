package cc.quarkus.qcc;

import java.io.IOException;

public class MyThrowingClass {

    public static int foo() {
        try {
            bar();
            bar();
        } catch (IllegalArgumentException e) {
            dump(e);
            return 2;
        }
        return 0;
    }

    public static void dump(Object o) {

    }
    public static void bar() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    public static void baz() throws IOException {
        throw new IOException();
    }
}
