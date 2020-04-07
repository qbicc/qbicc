package cc.quarkus.api;

/**
 *
 */
public final class Values {
    private Values() {
    }

    /**
     * Determine whether the given value expression is definitely constant.
     *
     * @param val the expression to test
     * @return {@code true} if the expression is constant in this context; {@code false} otherwise
     */
    public static native boolean isConstant(Object val);

    public static native boolean isConstant(long val);

    public static native boolean isConstant(int val);

    public static native boolean isConstant(short val);

    public static native boolean isConstant(byte val);

    public static native boolean isConstant(float val);

    public static native boolean isConstant(double val);

    public static native boolean isConstant(boolean val);

    public static native <T> boolean compareAndSwapVolatile(T expr, T expect, T update);

    public static native <T> boolean compareAndSwapAcquire(T expr, T expect, T update);

    public static native <T> boolean compareAndSwapRelease(T expr, T expect, T update);

    public static native <T> boolean compareAndSwap(T expr, T expect, T update);
    public static native <T> boolean compareAndSwap(int expr, int expect, int update);

    public static native <T> T getAndSetVolatile(T expr, T newValue);

    public static native <T> T getAndSetRelaxed(T expr, T newValue);

    public static native <T> void setVolatile(T expr, T newValue);

    public static native <T> void setRelaxed(T expr, T newValue);

    public static native <T> T getVolatile(T expr);

    public static native <T> T getRelaxed(T expr);

    public static native void exitConstructorBarrier(Object instance);

    public static native long fieldOffset(Object expr);

    public static native long fieldOffset(long expr);

    public static native long fieldOffset(double expr);

    public static native long fieldOffset(boolean expr);

    public static native long elementOffset(Object array, int index);
}
