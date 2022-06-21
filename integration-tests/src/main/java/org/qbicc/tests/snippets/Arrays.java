package org.qbicc.tests.snippets;

import static org.qbicc.runtime.CNative.*;

public class Arrays {
    @extern
    public static native int putchar(int arg);

    static class A {
        final int x;
        A(int x) { this.x = x; }
    }

    static A[] test1() {
        A[] as = new A[3];
        for (int i=0; i<as.length; i++) {
            as[i] = new A(10*i);
        }
        return as;
    }

    static int validate1(A[] data) {
        for (int i=0; i<data.length; i++) {
            if (data[i].x != i*10) {
                putchar('N');
                return 1;
            }
        }
        putchar('Y');
        return 0;
    }

    static A[][] test2(int x, int y) {
        A [][] data = new A[x][y];
        for (int i=0; i<x; i++) {
            for (int j=0; j<y; j++) {
                data[i][j] = new A(10*i + j);
            }
        }
        return data;
    }

    static int validate2(A[][] data) {
        int expected = 0;
        for (int i=0; i<10; i++) {
            for (int j=0; j<10; j++) {
                if (data[i][j].x != expected++) {
                    putchar('N');
                    return 1;
                }
            }
        }
        putchar('Y');
        return 0;
    }

    static int[][] test3(int x, int y) {
        int [][] data = new int[x][y];
        for (int i=0; i<x; i++) {
            for (int j=0; j<y; j++) {
                data[i][j] = 10*i + j;
            }
        }
        return data;
    }

    static int validate3(int[][] data) {
        int expected = 0;
        for (int i=0; i<10; i++) {
            for (int j=0; j<10; j++) {
                if (data[i][j] != expected++) {
                    putchar('N');
                    return 1;
                }
            }
        }
        putchar('Y');
        return 0;
    }

    public static void main(String[] args) {
        int failCount = 0;
        failCount += validate1(test1());
        failCount += validate2(test2(10,10));
        failCount += validate3(test3(10,10));
        putchar('\n');
    }
}
