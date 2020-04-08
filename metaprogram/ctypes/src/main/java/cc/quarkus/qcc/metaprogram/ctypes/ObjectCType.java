package cc.quarkus.qcc.metaprogram.ctypes;

/**
 * The base class for all C types.
 */
public interface ObjectCType {
    boolean isComplete();

    boolean isConst();

    int getSize();
}
