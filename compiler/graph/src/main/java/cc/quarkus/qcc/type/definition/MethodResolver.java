package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public interface MethodResolver {
    MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing);
}
