package cc.quarkus.vm.api;

import cc.quarkus.qcc.type.definition.TypeDefinition;

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

    TypeDefinition getTypeDefinition();
}
