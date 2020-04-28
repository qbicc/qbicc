package cc.quarkus.qcc.type;

public class Sentinel {

    public static class Void {

        public static final Void VOID = new Void();

        private Void() {

        }

        @Override
        public String toString() {
            return "<<void>>";
        }
    }

    public static class Null {
        public static final Null NULL = new Null();

        private Null() {

        }

        @Override
        public String toString() {
            return "<<null>>";
        }
    }
}
