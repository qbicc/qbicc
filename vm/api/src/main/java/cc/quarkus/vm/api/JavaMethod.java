package cc.quarkus.vm.api;

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

    JavaClass getDeclaringClass();

    String getMethodName();

    int getModifiers();

    String getSignature();
}
