package org.qbicc.runtime;

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

    public static native boolean isAlwaysTrue(boolean expr);
    public static native boolean isAlwaysFalse(boolean expr);

    public static native <T> boolean compareAndSetVolatile(T expr, T expect, T update);
    public static native boolean compareAndSetVolatile(int expr, int expect, int update);
    public static native boolean compareAndSetVolatile(long expr, long expect, long update);

    public static native <T> boolean compareAndSetAcquire(T expr, T expect, T update);
    public static native boolean compareAndSetAcquire(int expr, int expect, int update);
    public static native boolean compareAndSetAcquire(long expr, long expect, long update);

    public static native <T> boolean compareAndSetRelease(T expr, T expect, T update);
    public static native boolean compareAndSetRelease(int expr, int expect, int update);
    public static native boolean compareAndSetRelease(long expr, long expect, long update);

    public static native <T> boolean compareAndSet(T expr, T expect, T update);
    public static native boolean compareAndSet(int expr, int expect, int update);
    public static native boolean compareAndSet(long expr, long expect, long update);

    public static native <T> T getAndSetVolatile(T expr, T newValue);
    public static native int getAndSetVolatile(int expr, int newValue);
    public static native long getAndSetVolatile(long expr, long newValue);

    public static native <T> T getAndSetRelaxed(T expr, T newValue);
    public static native int getAndSetRelaxed(int expr, int newValue);
    public static native long getAndSetRelaxed(long expr, long newValue);

    public static native <T> void setVolatile(T expr, T newValue);
    public static native void setVolatile(int expr, int newValue);
    public static native void setVolatile(long expr, long newValue);

    public static native <T> void setRelaxed(T expr, T newValue);
    public static native void setRelaxed(int expr, int newValue);
    public static native void setRelaxed(long expr, long newValue);

    public static native <T> T getVolatile(T expr);
    public static native int getVolatile(int expr);
    public static native long getVolatile(long expr);

    public static native <T> T getRelaxed(T expr);
    public static native int getRelaxed(int expr);
    public static native long getRelaxed(long expr);

    public static native void exitConstructorBarrier(Object instance);
}
