package cc.quarkus.vm.api;

import cc.quarkus.qcc.type.definition.DefinedMethodDefinition;

/**
 * A Java method handle.
 */
public interface JavaMethod {
    default boolean isConstructor() {
        return false;
    }

    default JavaConstructor asConstructor() {
        throw new ClassCastException();
    }

    DefinedMethodDefinition getDefinition();

    JavaClass getDeclaringClass();

    String getMethodName();

    int getModifiers();

    String getSignature();
}
