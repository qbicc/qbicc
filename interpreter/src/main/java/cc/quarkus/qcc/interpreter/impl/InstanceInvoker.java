package cc.quarkus.qcc.interpreter.impl;

import cc.quarkus.qcc.interpreter.VmObject;

/**
 *
 */
interface InstanceInvoker {
    void invokeVoid(VmObject target, Object... args);
}
