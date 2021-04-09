package org.qbicc.interpreter;

@SuppressWarnings("serial")
public final class Thrown extends RuntimeException {
    private final VmThrowable throwable;

    public Thrown(final VmThrowable throwable) {
        this.throwable = throwable;
    }

    public VmThrowable getThrowable() {
        return throwable;
    }
}
