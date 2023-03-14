package org.qbicc.plugin.reachability;

import org.qbicc.facts.Fact;
import org.qbicc.type.definition.element.InitializerElement;

/**
 * The core set of initializer facts.
 */
public enum InitializerReachabilityFacts implements Fact<InitializerElement> {
    /**
     * The initializer needs to be executed.
     */
    NEEDS_INITIALIZATION,
    ;

    @Override
    public Class<InitializerElement> getElementType() {
        return InitializerElement.class;
    }
}
