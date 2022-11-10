package org.qbicc.tests.snippets;

import org.qbicc.runtime.CNative;

public class LoopTests {
    @CNative.extern
    public static native int putchar(int arg);

    static Object f1(JP x) { return x; }
    static Object[] f2(Object[] v) { return v; }

    static class JP {
        int count = 10;
        boolean cond() { return count-- > 0; }
    }

    // This test captures a pattern that resulted in qbicc generating invalid LLIR
    static Object[] test(JP jp) {
        Object[] values = new Object[1];
        int ptr = 0;
        do {
            Object value = f1(jp);
            if (ptr >= values.length) {
                values = f2(values);
                ptr = 0;
            }
            values[ptr++] = value;
        } while (jp.cond());
        return values;
    }

    public static void main(String[] args) {
        JP x = new JP();
        Object[] res = test(x);
        if (res[0] == x && x.count == -1) {
            putchar('Y');
        } else {
            putchar('N');
        }
    }
}
