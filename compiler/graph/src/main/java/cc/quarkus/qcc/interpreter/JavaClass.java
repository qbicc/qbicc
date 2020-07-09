package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

/**
 *
 */
public interface JavaClass extends JavaObject {
    default boolean isClass() {
        return true;
    }

    default JavaClass asClass() {
        return this;
    }

    VerifiedTypeDefinition getTypeDefinition();
}
