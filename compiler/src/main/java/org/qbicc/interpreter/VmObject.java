package org.qbicc.interpreter;

import org.qbicc.type.ClassObjectType;
import org.qbicc.type.PhysicalObjectType;

/**
 * A Java object handle.
 */
public interface VmObject {
    VmClass getVmClass();

    /**
     * Get the actual object type.
     *
     * @return the actual object type (must not be {@code null})
     */
    PhysicalObjectType getObjectType();

    /**
     * Get the object's type ID type value.  This will be different from the actual type for arrays.
     *
     * @return the object's type ID type value (must not be {@code null})
     */
    ClassObjectType getObjectTypeId();

    Memory getMemory();

    void monitorEnter();

    void monitorExit();

    void vmWait() throws InterruptedException;

    void vmWait(long millis) throws InterruptedException;

    void vmWait(long millis, int nanos) throws InterruptedException;

    void vmNotify();

    void vmNotifyAll();
}
