package cc.quarkus.vm.api;

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
