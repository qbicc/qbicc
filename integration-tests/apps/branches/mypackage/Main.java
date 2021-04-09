package mypackage;

import static org.qbicc.runtime.CNative.*;

public class Main {
    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
        putchar(1 == 1 ? '1' : '0');
        putchar(' ');
        int a = 1;
        int b = 1;
        putchar(a == b ? '1' : '0');
    }
}

