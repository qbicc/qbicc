package org.qbicc.interpreter;

@SuppressWarnings("serial")
public final class Thrown extends RuntimeException {
    private final VmThrowable throwable;

    public Thrown(final VmThrowable throwable) {
        this.throwable = throwable;
        setStackTrace(throwable.getStackTrace());
    }

    public VmThrowable getThrowable() {
        return throwable;
    }

    @Override
    public String getMessage() {
        return throwable.getMessage();
    }
}
