package mypackage;

import static cc.quarkus.qcc.runtime.CNative.*;

public class Main {
    @extern
    public static native int putchar(int arg);

    @export
    public static int main() {
        putchar(1 == 1 ? '1' : '0');
        putchar(' ');
        int a = 1;
        int b = 1;
        putchar(a == b ? '1' : '0');
        return 0;
    }

    public static void main(String[] args) {
        // make driver happy
    }
}

