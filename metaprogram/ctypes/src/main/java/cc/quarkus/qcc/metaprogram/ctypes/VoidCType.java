package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface VoidCType extends ObjectCType {
    default boolean isComplete() {
        return false;
    }
}
