package org.qbicc.type.definition;

import org.qbicc.type.definition.element.NestedClassElement;

/**
 *
 */
public interface EnclosedClassResolver {
    NestedClassElement resolveEnclosedNestedClass(int index, final DefinedTypeDefinition enclosing);
}
