package org.qbicc.plugin.reachability;

import org.qbicc.facts.Fact;
import org.qbicc.type.definition.LoadedTypeDefinition;

/**
 * The core set of type facts.
 */
public enum TypeReachabilityFacts implements Fact<LoadedTypeDefinition> {
    /**
     * The class corresponding to the type is reachable in some way, requiring a `Class` instance to be emitted.
     */
    HAS_CLASS,
    /**
     * The class is found on the heap, either via instantiation or via the reachable initial heap.
     */
    IS_ON_HEAP,
    /**
     * The class is explicitly instantiated by reachable code.
     */
    IS_INSTANTIATED,
    /**
     * One or more methods of this class are provisionally directly invoked.
     */
    ELEMENT_IS_PROVISIONALLY_INVOKED,
    /**
     * One or more methods of this class are provisionally dispatch invoked.
     */
    ELEMENT_IS_PROVISIONALLY_DISPATCH_INVOKED,
    /**
     * A subtype of this type {@linkplain #HAS_CLASS has a present class}.
     */
    SUBTYPE_HAS_CLASS,
    ;

    @Override
    public Class<LoadedTypeDefinition> getElementType() {
        return LoadedTypeDefinition.class;
    }
}
