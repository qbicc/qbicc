package org.qbicc.interpreter.impl;

import org.qbicc.interpreter.VmObject;

/**
 *
 */
interface InstanceInvoker {
    void invokeVoid(VmObject target, Object... args);
}
