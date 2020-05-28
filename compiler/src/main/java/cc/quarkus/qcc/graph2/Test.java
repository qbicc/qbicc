package cc.quarkus.qcc.graph2;

/**
 *
 */
public class Test {
    int bzt;
    static Object blart;

    public int foo(int blah) {
        int foo = blah;
        while (foo < 100) {
            if (blah < 10) {
                return 14 + blah;
            } else {
                foo = foo + 10;
                foo = 11 + foo;
                bzt = foo;
                foo = 99 + foo & blah;
                blart = Integer.valueOf(foo);
            }
        }
        return foo;
    }
}
