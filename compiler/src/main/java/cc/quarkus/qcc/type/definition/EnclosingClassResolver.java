package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.NestedClassElement;

/**
 *
 */
public interface EnclosingClassResolver {
    NestedClassElement resolveEnclosingNestedClass(int index, DefinedTypeDefinition enclosed);
}
