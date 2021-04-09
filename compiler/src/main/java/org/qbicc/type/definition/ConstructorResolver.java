package org.qbicc.type.definition;

import org.qbicc.type.definition.element.ConstructorElement;

/**
 *
 */
public interface ConstructorResolver {
    ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing);
}
