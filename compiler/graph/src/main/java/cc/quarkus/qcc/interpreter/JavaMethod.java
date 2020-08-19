package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.definition.MethodHandle;

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

    MethodHandle getExactHandle();

    MethodHandle getVirtualHandle();

    JavaClass getDeclaringClass();

    String getMethodName();

    int getModifiers();
}
