import static cc.quarkus.qcc.runtime.CNative.*;

public class Example1 {
    @extern
    public static native int putchar(int arg);

    @export
    public static int main() {
        putchar('H');
        putchar('A');
        putchar('\n');
        putchar('H');
        putchar('A');
        putchar('\n');
        return 0;
    }

    public static void main(String[] args) {
        // make driver happy
    }
}