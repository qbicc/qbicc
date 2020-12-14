package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.NestedClassElement;

/**
 *
 */
public interface EnclosedClassResolver {
    NestedClassElement resolveEnclosedNestedClass(int index, final DefinedTypeDefinition enclosing);
}
