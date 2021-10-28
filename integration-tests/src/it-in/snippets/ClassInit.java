import static org.qbicc.runtime.CNative.*;

public class ClassInit {
    static boolean parentInitialized;
    static boolean childInitialized;
    static boolean holderInitialized;
    static boolean iInitialized;
    static boolean jInitialized;
    static boolean kInitialized;

    static class Parent {
        static {
            parentInitialized = true;
        }

        public static void call_child() {
            Child.method();
        }
    }

    static class Child {
        static {
            childInitialized = true;
        }

        public static void method() { }
    }

    static class FieldHolder {
        static {
            holderInitialized = true;
        }

        public static String s;
        static {
            s = new String("a");
        }
    }

    static interface I {
        String s = initS();

        static String initS() {
            iInitialized = true;
            return new String("b");
        }
        default void m() { }
    }

    static interface J extends I {
        String s = initS();

        static String initS() {
            // should not be called by class init of implmentor
            jInitialized = true;
            return new String("b");
        }
    }

    static class K implements J {
        static {
            kInitialized = true;
        }

    }

    @extern
    public static native int putchar(int arg);

    public static void main(String[] args) {
       Parent.call_child();
       if (parentInitialized) putchar('P');
       if (childInitialized) putchar('C');
       putchar('#');
       String s = FieldHolder.s;
       if (holderInitialized) putchar('F');
       if (s == null) {
           throw new NullPointerException("Ensure field get isn't removed");
       }
       putchar('#');
       new K();
       if (iInitialized) putchar('I');
       if (jInitialized) putchar('J');
       if (kInitialized) putchar('K');
    }


}
