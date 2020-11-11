package cc.quarkus.qcc.interpreter;

/**
 *
 */
public interface VmArray extends VmObject {
    int getLength();

    VmClass getNestedType();

    boolean getArrayBoolean(int index);

    int getArrayInt(int index);

    long getArrayLong(int index);

    VmObject getArrayObject(int index);

    void putArray(int index, boolean value);

    void putArray(int index, int value);

    void putArray(int index, long value);

    void putArray(int index, VmObject value);
}
