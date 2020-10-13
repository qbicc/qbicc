package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.graph.literal.RealTypeIdLiteral;

/**
 * A Java object handle.
 */
public interface JavaObject {

    RealTypeIdLiteral getObjectType();
}
