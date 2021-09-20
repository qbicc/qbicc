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
        putchar('#');
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
        putchar('#');
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
        putchar('#');
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
        putchar('#');
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
        putchar('#');
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
        putchar('#');
    }
    public static void main(String args[]) {
        testPrimitiveType();
        testArrayOfPrimitiveType();
        testMultiDimensionalArrayOfPrimitiveType();
        testConcreteType();
        testArrayOfConcreteType();
        testMultiDimensionalArrayOfConcreteType();
    }
}