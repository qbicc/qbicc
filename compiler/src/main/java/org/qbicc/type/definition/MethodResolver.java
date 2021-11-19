package org.qbicc.type.definition;

import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public interface MethodResolver {
    MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder);
}
