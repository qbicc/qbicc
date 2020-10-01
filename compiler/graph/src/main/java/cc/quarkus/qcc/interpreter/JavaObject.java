package cc.quarkus.qcc.interpreter;

/**
 * A Java object handle.
 */
public interface JavaObject {

    default boolean isClass() {
        return false;
    }

    default JavaClass asClass() {
        throw new ClassCastException();
    }

    default boolean isArray() {
        return false;
    }

    default JavaArray asArray() {
        throw new ClassCastException();
    }

    default boolean isPrimitiveClass() {
        return false;
    }

    default JavaPrimitiveClass asPrimitiveClass() {
        throw new ClassCastException();
    }
}
