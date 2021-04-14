// https://github.com/qbicc/qbicc/pull/219
import static org.qbicc.runtime.CNative.*;

public class ArithmeticNegation {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
        byte b = 10;
        int ib = -b;
        putchar(-b == ib ? 'X' : 'F');
    }
}