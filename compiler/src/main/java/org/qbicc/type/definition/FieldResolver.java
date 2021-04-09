package org.qbicc.type.definition;

import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public interface FieldResolver {
    FieldElement resolveField(int index, DefinedTypeDefinition enclosing);
}
