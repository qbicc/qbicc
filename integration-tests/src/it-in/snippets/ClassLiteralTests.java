import static org.qbicc.runtime.CNative.*;

class Foo {};

public class ClassLiteralTests {
    @extern
    public static native int putchar(int arg);

    static Class<?> dummy(Class<?> cls) {
        return cls;
    }

    static void testPrimitiveType() {
        Class<?> cls = dummy(int.class);
        if (cls.equals(int.class)) {
            putchar('P');
        } else {
            putchar('F');
        }
        if (cls.getName().equals("int")) {
            putchar('P');
        } else {
            putchar('F');
        }
    }

    static void testArrayOfPrimitiveType() {
        Class<?> cls = dummy(int[].class);
        if (cls.equals(int[].class)) {
            putchar('P');
        } else {
            putchar('F');
        }
        if (cls.getName().equals("[I")) {
            putchar('P');
        } else {
            putchar('F');
        }
    }

    static void testMultiDimensionalArrayOfPrimitiveType() {
        Class<?> cls = dummy(int[][].class);
        if (cls.equals(int[][].class)) {
            putchar('P');
        } else {
            putchar('F');
        }
        if (cls.getName().equals("[[I")) {
            putchar('P');
        } else {
            putchar('F');
        }
    }

    static void testConcreteType() {
        Class<?> cls = dummy(Foo.class);
        if (cls.equals(Foo.class)) {
            putchar('P');
        } else {
            putchar('F');
        }
        if (cls.getName().equals("Foo")) {
            putchar('P');
        } else {
            putchar('F');
        }
    }

    static void testArrayOfConcreteType() {
        Class<?> cls = dummy(Foo[].class);
        if (cls.equals(Foo[].class)) {
            putchar('P');
        } else {
            putchar('F');
        }
        if (cls.getName().equals("[LFoo;")) {
            putchar('P');
        } else {
            putchar('F');
        }
    }

    static void testMultiDimensionalArrayOfConcreteType() {
        Class<?> cls = dummy(Foo[][].class);
        if (cls.equals(Foo[][].class)) {
            putchar('P');
        } else {
            putchar('F');
        }
        if (cls.getName().equals("[[LFoo;")) {
            putchar('P');
        } else {
            putchar('F');
        }
    }
    public static void main(String args[]) {
        /**
         * Tests for class literals of primitive type are disabled as
         * they need static reference fields to be initialized properly.
         * Currently the static reference fields are set to null.
         */
        //testPrimitiveType();
        //testArrayOfPrimitiveType();
        //testMultiDimensionalArrayOfPrimitiveType();
        testConcreteType();
        testArrayOfConcreteType();
        testMultiDimensionalArrayOfConcreteType();
    }
}