package org.qbicc.type.definition;

import org.qbicc.type.definition.element.NestedClassElement;

/**
 *
 */
public interface EnclosingClassResolver {
    NestedClassElement resolveEnclosingNestedClass(int index, DefinedTypeDefinition enclosed);
}
