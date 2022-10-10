package org.qbicc.tests.snippets;

import java.util.ServiceLoader;

public class ServiceLoading {
    public interface MyService {
        int value();
    }

    public static class MyImpl1 implements  MyService {
        public int value() {
            return 100;
        }
    }

    public static class MyImpl2 implements  MyService {
        public int value() {
            return 10;
        }
    }

    public static void main(String[] args) {
        int sum = 0;
        for (MyService s: ServiceLoader.load(MyService.class)) {
            sum += s.value();
        }
        System.out.print(sum);
    }
}
