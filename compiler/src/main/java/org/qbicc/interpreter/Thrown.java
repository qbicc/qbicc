package org.qbicc.interpreter;

@SuppressWarnings("serial")
public final class Thrown extends RuntimeException {
    private final VmThrowable throwable;
    private final StackTraceElement[] realStackTrace;

    public Thrown(final VmThrowable throwable) {
        this.throwable = throwable;
        realStackTrace = getStackTrace();
        setStackTrace(throwable.getStackTrace());
        VmThrowable cause = throwable.getCause();
        if (cause != null) {
            initCause(new Thrown(cause));
        }
    }

    public VmThrowable getThrowable() {
        return throwable;
    }

    @Override
    public String getMessage() {
        final String message = throwable.getMessage();
        String className = throwable.getVmClass().getName();
        return message == null ? className : className + ": " + message;
    }

    @Override
    public String toString() {
        return "(interpreter) " + getMessage();
    }
}
