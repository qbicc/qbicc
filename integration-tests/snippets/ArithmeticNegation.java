// https://github.com/quarkuscc/qcc/pull/219
import static cc.quarkus.qcc.runtime.CNative.*;

public class ArithmeticNegation {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
        byte b = 10;
        int ib = -b;
        putchar(-b == ib ? 'X' : 'F');
    }
}