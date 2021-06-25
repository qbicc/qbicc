import static org.qbicc.runtime.CNative.*;

public class ClassInit {

    static class Parent {
        static {
            putchar('P');
        }

        public static void call_child() {
            Child.method();
        }
    }

    static class Child {
        static {
            putchar('C');
        }

        public static void method() { }
    }

    static class FieldHolder {
        static {
            putchar('F');
        }

        public static String s;
        static {
            s = new String("a");
        }
    }

    static interface I {
        String s = initS();

        static String initS() {
            putchar('I');
            return new String("b");
        }
        default void m() { }
    }

    static interface J extends I {
        String s = initS();

        static String initS() {
            putchar('J'); // should not be called by class init of implmentor
            return new String("b");
        }
    }

    static class K implements J {
        static {
            putchar('K');
        }

    }

    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
       Parent.call_child();
       putchar('#');
       String s = FieldHolder.s;
       if (s == null) {
           throw new NullPointerException("Ensure field get isn't removed");
       }
       putchar('#');
       new K();
    }


}
