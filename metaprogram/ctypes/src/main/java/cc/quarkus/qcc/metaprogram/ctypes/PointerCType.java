package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface PointerCType extends PointerLikeCType {
    default boolean isPointer() {
        return true;
    }

    default PointerCType asPointer() {
        return this;
    }
}
