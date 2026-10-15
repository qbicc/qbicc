package org.qbicc.facts.core;

import org.qbicc.facts.Fact;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * The core set of facts which apply to any executable element.
 */
public enum ExecutableReachabilityFacts implements Fact<ExecutableElement> {
    /**
     * This element is definitely directly or indirectly invoked.
     */
    IS_INVOKED,
    /**
     * This element must be compiled.
     */
    NEEDS_COMPILATION,
    /**
     * This element has been compiled.
     */
    IS_COMPILED,
    ;

    @Override
    public Class<ExecutableElement> getElementType() {
        return ExecutableElement.class;
    }
}
