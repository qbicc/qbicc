package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface PointerLikeCType extends WordCType {
    boolean isRestrict();

    ObjectCType getEnclosedType();
}
