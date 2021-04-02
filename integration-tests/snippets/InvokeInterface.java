import static cc.quarkus.qcc.runtime.CNative.*;

public class InvokeInterface {
    static interface J {
        int m2();
    }

    static interface I extends J {
        int m1();
    }

    static class C implements I {
        public int m1() { return m2() + m3(); }
        public int m2() { return m4(); }
        int m3() { return 40; }
        int m4() { return 20; }
    }

    @extern
    public static native int putchar(int arg);

    static I alloc() { return new C(); }

    static void reportSuccess() {
        putchar('P');
        putchar('A');
        putchar('S');
        putchar('S');
        putchar('\n');
    }

    static void reportFailure() {
        putchar('F');
        putchar('A');
        putchar('I');
        putchar('L');
        putchar('\n');
    }

    public static void main(String[] args) {
        I it = alloc();
        int a = it.m1();
        int b = it.m2();
        if (a == 60 && b == 20) {
            reportSuccess();
        } else {
            reportFailure();
        }
    }
}
