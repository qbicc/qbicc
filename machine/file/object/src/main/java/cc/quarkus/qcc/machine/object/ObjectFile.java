package cc.quarkus.qcc.machine.object;

import java.io.Closeable;
import java.nio.ByteOrder;

import cc.quarkus.qcc.machine.arch.Cpu;
import cc.quarkus.qcc.machine.arch.ObjectType;

/**
 * A generic API to introspect object files in a format-agnostic manner.
 */
public interface ObjectFile extends Closeable {
    int getSymbolValueAsByte(String name);

    int getSymbolValueAsInt(String name);

    long getSymbolValueAsLong(String name);

    byte[] getSymbolAsBytes(String name, int size);

    String getSymbolValueAsUtfString(String name);

    long getSymbolSize(String name);

    ByteOrder getByteOrder();

    Cpu getCpu();

    ObjectType getObjectType();
}
