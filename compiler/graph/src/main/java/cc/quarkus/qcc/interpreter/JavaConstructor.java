package cc.quarkus.qcc.interpreter;

/**
 *
 */
public interface JavaConstructor extends JavaMethod {
    default boolean isConstructor() {
        return true;
    }

    default JavaConstructor asConstructor() {
        return this;
    }
}
