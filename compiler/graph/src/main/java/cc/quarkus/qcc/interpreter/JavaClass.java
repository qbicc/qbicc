package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

/**
 *
 */
public interface JavaClass extends JavaObject {

    VerifiedTypeDefinition getTypeDefinition();
}
