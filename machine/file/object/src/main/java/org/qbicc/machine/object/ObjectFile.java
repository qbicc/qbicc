package org.qbicc.machine.object;

import java.io.Closeable;
import java.nio.ByteOrder;

import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;

/**
 * A generic API to introspect object files in a format-agnostic manner.
 */
public interface ObjectFile extends Closeable {
    int getSymbolValueAsByte(String name);

    int getSymbolValueAsInt(String name);

    long getSymbolValueAsLong(String name);

    byte[] getSymbolAsBytes(String name, int size);

    String getSymbolValueAsUtfString(String name, int nbytes);

    long getSymbolSize(String name);

    ByteOrder getByteOrder();

    Cpu getCpu();

    ObjectType getObjectType();

    Section getSection(String name);

    String getRelocationSymbolForSymbolValue(String symbol);

    String getStackMapSectionName();
}
