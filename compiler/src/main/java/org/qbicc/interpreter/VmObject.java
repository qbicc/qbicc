package org.qbicc.interpreter;

import org.qbicc.type.ClassObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.definition.element.FieldElement;

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

    /**
     * Get the known index of the given field on this object.
     *
     * @param field the field (must not be {@code null})
     * @return the index
     * @throws IllegalArgumentException if the field does not belong on this instance
     */
    int indexOf(FieldElement field) throws IllegalArgumentException;
}
