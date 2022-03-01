package org.qbicc.plugin.reachability;

import org.qbicc.facts.Fact;
import org.qbicc.interpreter.VmObject;

/**
 * The core set of heap object facts.
 */
public enum ObjectReachabilityFacts implements Fact<VmObject> {
    /**
     * The heap object is reachable from an object literal, a static field value, or a reference from another reachable
     * heap object.
     */
    IS_REACHABLE,
    ;

    @Override
    public Class<VmObject> getElementType() {
        return VmObject.class;
    }
}
