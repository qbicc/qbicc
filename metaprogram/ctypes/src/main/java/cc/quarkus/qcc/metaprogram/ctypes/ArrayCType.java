package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface ArrayCType extends PointerLikeCType {
    default boolean isArray() {
        return true;
    }

    default ArrayCType asArray() {
        return this;
    }

    int getDimension();
}
