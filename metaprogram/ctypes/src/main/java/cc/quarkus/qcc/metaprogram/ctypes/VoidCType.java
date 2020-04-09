package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface VoidCType extends ObjectCType {
    default boolean isVoid() {
        return true;
    }

    default VoidCType asVoid() {
        return this;
    }

    default boolean isComplete() {
        return false;
    }
}
