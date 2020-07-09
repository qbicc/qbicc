package cc.quarkus.qcc.interpreter;

/**
 *
 */
public interface JavaArray extends JavaObject {
    int dimensions();

    default boolean isArray() {
        return true;
    }

    default JavaArray asArray() {
        return this;
    }

    JavaClass getNestedType();

    JavaArray getArrayArray(int index);

    JavaObject getArrayObject(int index);

    long getArrayLong(int index);

    int getArrayInt(int index);

    byte getArrayByte(int index);

    boolean getArrayBoolean(int index);
}
