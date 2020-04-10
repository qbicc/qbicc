package cc.quarkus.vm.api;

/**
 *
 */
public interface JavaPrimitiveClass extends JavaClass {
    default boolean isPrimitiveClass() {
        return true;
    }

    default JavaPrimitiveClass asPrimitiveClass() {
        return this;
    }
}
