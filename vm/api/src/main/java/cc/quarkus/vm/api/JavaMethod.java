package cc.quarkus.vm.api;

/**
 * A Java method handle.
 */
public interface JavaMethod {
    JavaClass getDeclaringClass();

    String getMethodName();

    int getModifiers();

    String getSignature();
}
