package cc.quarkus.qcc;

public class MyClass {

    public int sum(int a, int b) {
        while ( a < b ) {
            a = a + 1;
        }

        return a;
    }

    public int foo() {
        return 89;
    }

    public static int min(int a, int b) {
        if ( a < b ) {
            return a;
        }

        return b;
    }
}
