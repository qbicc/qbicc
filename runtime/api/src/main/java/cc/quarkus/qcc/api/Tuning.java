package cc.quarkus.qcc.api;

/**
 *
 */
public final class Tuning {
    private Tuning() {}

    public static <T> T mayBeNull(T val, float likelihood) {
        return val;
    }

    public static <T> T mayBeNonNull(T val, float likelihood) {
        return val;
    }

    public static boolean mayBeTrue(boolean cond, float likelihood) {
        return cond;
    }

    public static boolean mayBeFalse(boolean cond, float likelihood) {
        return cond;
    }
}
