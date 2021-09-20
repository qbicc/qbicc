package org.qbicc.interpreter;

import org.qbicc.type.PhysicalObjectType;

/**
 * A Java object handle.
 */
public interface VmObject {
    VmClass getVmClass();

    PhysicalObjectType getObjectType();

    Memory getMemory();

    void monitorEnter();

    void monitorExit();

    void vmWait() throws InterruptedException;

    void vmWait(long millis) throws InterruptedException;

    void vmWait(long millis, int nanos) throws InterruptedException;

    void vmNotify();

    void vmNotifyAll();
}
