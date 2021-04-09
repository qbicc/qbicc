package org.qbicc.type.definition;

import org.qbicc.type.definition.element.InitializerElement;

/**
 *
 */
public interface InitializerResolver {

    InitializerElement resolveInitializer(int index, DefinedTypeDefinition enclosing);
}
