import static cc.quarkus.qcc.runtime.CNative.*;

public class Example1 {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
        putchar('H');
        putchar('A');
        putchar('\n');
        putchar('H');
        putchar('A');
        putchar('\n');
    }
}