package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.ConstructorElement;

/**
 *
 */
public interface ConstructorResolver {
    ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing);
}
