package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface PointerLikeCType extends WordCType {
    default boolean isPointerLike() {
        return true;
    }

    default PointerLikeCType asPointerLike() {
        return this;
    }

    boolean isRestrict();

    ObjectCType getEnclosedType();
}
