package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.type.PhysicalObjectType;

/**
 * A Java object handle.
 */
public interface VmObject {
    VmClass getVmClass();

    PhysicalObjectType getObjectType();

    VmObject readField(int fieldIndex);

    boolean readFieldBoolean(int fieldIndex);

    int readFieldInt(int fieldIndex);

    long readFieldLong(int fieldIndex);

    float readFieldFloat(int fieldIndex);

    double readFieldDouble(int fieldIndex);

    void writeField(int fieldIndex, VmObject value);

    void writeField(int fieldIndex, boolean value);

    void writeField(int fieldIndex, int value);

    void writeField(int fieldIndex, long value);

    void writeField(int fieldIndex, float value);

    void writeField(int fieldIndex, double value);
}
