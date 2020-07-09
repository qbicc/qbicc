package cc.quarkus.qcc.interpreter;

/**
 * A primitive class (or {@code void}).
 */
public interface JavaPrimitiveClass extends JavaClass {
    default boolean isPrimitiveClass() {
        return true;
    }

    default JavaPrimitiveClass asPrimitiveClass() {
        return this;
    }
}
