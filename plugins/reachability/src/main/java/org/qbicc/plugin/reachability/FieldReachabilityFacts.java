package org.qbicc.plugin.reachability;

import org.qbicc.facts.Fact;
import org.qbicc.type.definition.element.FieldElement;

/**
 * The core set of field facts.
 */
public enum FieldReachabilityFacts implements Fact<FieldElement> {
    /**
     * The field is touched by a read operation in reachable code.
     */
    IS_READ,
    /**
     * The field is touched by a write operation in reachable code.
     */
    IS_WRITTEN,
    ;

    @Override
    public Class<FieldElement> getElementType() {
        return FieldElement.class;
    }
}
