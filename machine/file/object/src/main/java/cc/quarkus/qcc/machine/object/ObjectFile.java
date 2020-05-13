package cc.quarkus.qcc.machine.object;

import java.io.Closeable;

/**
 * A generic API to introspect object files in a format-agnostic manner.
 */
public interface ObjectFile extends Closeable {
    int getSymbolValueAsInt(String name);

    long getSymbolValueAsLong(String name);

    String getSymbolValueAsUtfString(String name);

    long getSymbolSize(String name);
}
