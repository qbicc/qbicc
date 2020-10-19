package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;

/**
 *
 */
public interface JavaClass extends JavaObject {

    ValidatedTypeDefinition getTypeDefinition();
}
