// https://github.com/quarkuscc/qcc/pull/219
import static cc.quarkus.qcc.runtime.CNative.*;

public class MathMinMax {
    @extern
    public static native int putchar(int arg);

    @export
    public static int main() {
        int i1 = 1;
        int i2 = 2147483647;
        int ib1 = Math.min(i1, i2);
        putchar(ib1 == 1 ? 'X' : 'F');
        int ib2 = Math.max(i1, i2);
        putchar(ib2 == 2147483647 ? 'X' : 'F');

        long l1 = 1;
        long l2 = 9223372036854775807L;
        long lb1 = Math.min(l1, l2);
        putchar(lb1 == 1 ? 'X' : 'F');
        long lb2 = Math.max(l1, l2);
        putchar(lb2 == 9223372036854775807L ? 'X' : 'F');

        float f1 = 1.0f;
        float f2 = 3.4028235E38f;
        float fb1 = Math.min(f1, f2);
        putchar(fb1 == 1.0f ? 'X' : 'F');
        float fb2 = Math.max(f1, f2);
        putchar(fb2 == 3.4028235E38f ? 'X' : 'F');

        double d1 = 1.0;
        double d2 = 1.7976931348623157E308;
        double db1 = Math.min(d1, d2);
        putchar(db1 == 1 ? 'X' : 'F');
        double db2 = Math.max(d1, d2);
        putchar(db2 == 1.7976931348623157E308 ? 'X' : 'F');

        return 0;
    }

    public static void main(String[] args) {
        // make driver happy
    }
}