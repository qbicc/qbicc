package org.qbicc.interpreter;

/**
 * A VM throwable exception type.
 */
public interface VmThrowable extends VmObject {
    VmThrowableClass getVmClass();

    String getMessage();

    VmThrowable getCause();

    StackTraceElement[] getStackTrace();

    void prepareForSerialization();
}
