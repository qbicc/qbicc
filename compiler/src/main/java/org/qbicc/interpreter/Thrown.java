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
        return throwable.getMessage();
    }

    @Override
    public String toString() {
        String message = getMessage();
        String className = throwable.getVmClass().getName();
        if (message != null) {
            return "(interpreter) " + className + ": " + message;
        } else {
            return "(interpreter) " + className;
        }
    }
}
