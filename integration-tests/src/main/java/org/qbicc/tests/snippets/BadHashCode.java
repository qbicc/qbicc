package org.qbicc.tests.snippets;

import static org.qbicc.runtime.CNative.extern;

import java.util.HashMap;

public class BadHashCode {
    @extern
    public static native int putchar(int arg);

    static final class BadHash {
        public int hashCode() { return 0; }
    }

    public static void main(String[] args) {
        HashMap<BadHash,String> map = new HashMap<>();
        try {
            for (int i = 0; i < 20; i ++) {
                map.put(new BadHash(), "v" + i);
            }
            putchar('Y');
        } catch (Throwable t) {
            putchar('N');
        }
    }
}
