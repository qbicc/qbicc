package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.PhysicalObjectType;

/**
 * A Java object handle.
 */
public interface VmObject {

    PhysicalObjectType getObjectType();
}
