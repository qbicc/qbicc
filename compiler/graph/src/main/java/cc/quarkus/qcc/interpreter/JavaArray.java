package cc.quarkus.qcc.interpreter;

/**
 *
 */
public interface JavaArray extends JavaObject {
    int getLength();

    JavaClass getNestedType();

    boolean getArrayBoolean(int index);

    int getArrayInt(int index);

    long getArrayLong(int index);

    JavaObject getArrayObject(int index);

    void putArray(int index, boolean value);

    void putArray(int index, int value);

    void putArray(int index, long value);

    void putArray(int index, JavaObject value);
}
