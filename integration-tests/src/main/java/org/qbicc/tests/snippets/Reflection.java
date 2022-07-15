package org.qbicc.tests.snippets;

import org.qbicc.runtime.CNative;
import org.qbicc.runtime.ReflectivelyAccessed;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflection {
    // One of the goals of this class is to make sure that simply mentioning
    // the Class literal in reachable code is enough to ensure that all
    // @ReflectivelyAccessed members of the class are usable at Runtime.
    // Do not add any direct usages of methods of this class to the test program!
    static class Calculator {
        int accum;

        @ReflectivelyAccessed
        Calculator(int x) {
            accum = x;
        }

        @ReflectivelyAccessed
        void plus(int x) {
            accum += x;
        }

        @ReflectivelyAccessed
        void plus(int... xs) {
            for (int x : xs) {
                accum += x;
            }
        }
    }

    @CNative.extern
    public static native int putchar(int arg);

    public static void reportResult(boolean result) {
        if (result) {
            putchar('P'); // pass
        } else {
            putchar('F'); // fail
        }
    }

    public static boolean testCalculator(int initialValue) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<Calculator> ctor = Calculator.class.getDeclaredConstructor(int.class);
        Method plusOne = Calculator.class.getDeclaredMethod("plus", int.class);
        Method plusMany = Calculator.class.getDeclaredMethod("plus", int[].class);
        Calculator c = ctor.newInstance(initialValue);
        plusOne.invoke(c, 10);
        plusMany.invoke(c, new int[] {1,2,3,4,5,6});
        return c.accum == initialValue + 10 + 3 * 7;
    }

    public static void main(String[] args) {
        // TODO: Fix qbicc's reachability analysis.
        //       This code block should not be needed!
        Calculator dummy = new Calculator(50);
        dummy.plus(3);
        dummy.plus(3, 4, 5);
        // end TODO
        try {
            reportResult(testCalculator(100));
        } catch (Exception e) {
            reportResult(false);
        }
    }

}
