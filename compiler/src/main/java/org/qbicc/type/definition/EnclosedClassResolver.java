package org.qbicc.type.definition;

import org.qbicc.type.definition.element.NestedClassElement;

/**
 *
 */
public interface EnclosedClassResolver {
    NestedClassElement resolveEnclosedNestedClass(int index, DefinedTypeDefinition enclosing, NestedClassElement.Builder builder);
}
