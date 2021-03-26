import static cc.quarkus.qcc.runtime.CNative.*;

public class DynamicTypeTests {
    static class Top {}
    static class Middle extends Top implements I {}
    static class Bottom_1 extends Middle {}
    static class Bottom_2 extends Middle implements J {}

    static interface I {}
    static interface J {}

    @extern
    public static native int putchar(int arg);

    static void print(boolean x) {
        if (x) {
            putchar('y');
        } else {
            putchar('n');
        }
    }

    static void testInstanceOf(Object x) {
        print(x instanceof Top);
        print(x instanceof Middle);
        print(x instanceof Bottom_1);
        print(x instanceof Bottom_2);
        print(x instanceof I);
        print(x instanceof J);
        print(x instanceof Object[]);
        print(x instanceof int[]);
        print(x instanceof Bottom_1[]);
        print(x instanceof I[]);
        print(x instanceof Middle[][]);
        print(x instanceof J[][]);
        putchar('#');
    }

    @export
    public static int main() {
        testInstanceOf(new Bottom_1());
        testInstanceOf(new Middle[1]);
        testInstanceOf(new I[1][1]);
        testInstanceOf(new Bottom_1[1]);
        testInstanceOf(new Bottom_2[1][1]);
        return 0;
    }

    public static void main(String[] args) {
        main();
    }
}
