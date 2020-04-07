package cc.quarkus.qcc;

public class MyClass {

    public int sum(int a, int b) {
        int c = 0;
        if ( a < b ) {
            c = 1+a;
        } else {
            c = 2+b;
        }
        return c;
    }
}
