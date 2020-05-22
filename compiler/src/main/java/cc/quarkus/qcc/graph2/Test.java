package cc.quarkus.qcc.graph2;

/**
 *
 */
public class Test {
    public int foo(int blah) {
        int foo = blah;
        while (foo < 100) {
            if (blah < 10) {
                return 14 + blah;
            } else {
                foo = foo + 10;
            }
        }
        return foo;
    }
}
