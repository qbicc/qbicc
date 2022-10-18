package org.qbicc.plugin.reachability;

import org.qbicc.facts.Fact;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 * The core set of facts which apply to instance methods.
 */
public enum InstanceMethodReachabilityFacts implements Fact<InstanceMethodElement> {
    /**
     * The exact enclosing type is present on the heap.
     */
    EXACT_RECEIVER_IS_ON_HEAP,
    /**
     * At least one object is on the heap which would select this method.
     */
    DISPATCH_RECEIVER_IS_ON_HEAP,
    /**
     * This method might be directly invoked upon by reachable code, dependent upon {@link #EXACT_RECEIVER_IS_ON_HEAP}.
     * Only applies to instance methods.
     */
    IS_PROVISIONALLY_INVOKED,
    /**
     * This method is dispatch invoked upon (virtual or interface), dependent upon {@link #DISPATCH_RECEIVER_IS_ON_HEAP}.
     * Only applies to instance methods.
     */
    IS_PROVISIONALLY_DISPATCH_INVOKED,
    /**
     * This method is dispatch invoked upon (virtual or interface). Only applies to non-static methods.
     */
    IS_DISPATCH_INVOKED,
    ;

    @Override
    public Class<InstanceMethodElement> getElementType() {
        return InstanceMethodElement.class;
    }
}
