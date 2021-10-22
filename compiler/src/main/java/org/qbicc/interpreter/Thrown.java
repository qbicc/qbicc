package org.qbicc.interpreter;

@SuppressWarnings("serial")
public final class Thrown extends RuntimeException {
    private final VmThrowable throwable;

    public Thrown(final VmThrowable throwable) {
        this.throwable = throwable;
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
