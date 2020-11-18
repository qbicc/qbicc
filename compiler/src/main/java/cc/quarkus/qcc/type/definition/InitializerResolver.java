package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.InitializerElement;

/**
 *
 */
public interface InitializerResolver {

    InitializerElement resolveInitializer(int index, DefinedTypeDefinition enclosing);
}
