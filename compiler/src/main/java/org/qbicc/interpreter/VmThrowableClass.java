package org.qbicc.interpreter;

/**
 *
 */
public interface VmThrowableClass extends VmClass {
    VmThrowable newInstance(String message);

    VmThrowable newInstance(VmThrowable cause);

    VmThrowable newInstance(String message, VmThrowable cause);
}
