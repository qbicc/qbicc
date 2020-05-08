package cc.quarkus.qcc;

public class MyThrowingClass {

    public static int foo() {
        try {
            bar();
        } catch (IllegalArgumentException e) {
            dump(e);
            //return 2;
        }
        return 42;
    }

    public static void dump(Object o) {

    }

    public static void bar() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    /*
    public static void baz() throws IOException {
        throw new IOException();
    }
     */

    public static void crap() {
        try {
            try {
                dump(null);
            } catch (IllegalArgumentException iae) {

            } finally {
                dump(null);
            }
        } catch (RuntimeException ioe) {

        }

    }
}
