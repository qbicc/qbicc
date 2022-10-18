package org.qbicc.facts.core;

import org.qbicc.facts.Fact;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * The core set of facts which apply to any executable element.
 */
public enum ExecutableReachabilityFacts implements Fact<ExecutableElement> {
    /**
     * This element is definitely directly invoked.
     */
    IS_INVOKED,
    ;

    @Override
    public Class<ExecutableElement> getElementType() {
        return ExecutableElement.class;
    }
}
