package org.qbicc.machine.vio;

public interface ExceptionObjLongToLongFunction<T, E extends Exception> {
    long applyAsLong(T arg1, long arg2) throws E;
}
