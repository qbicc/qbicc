package cc.quarkus.qcc.metaprogram.ctypes;

/**
 * The base class for all C types.
 */
public interface ObjectCType {
    boolean isComplete();

    boolean isConst();

    default boolean isArray() {
        return false;
    }

    default ArrayCType asArray() {
        throw new ClassCastException();
    }

    default boolean isCompound() {
        return false;
    }

    default CompoundCType asCompound() {
        throw new ClassCastException();
    }

    default boolean isPointerLike() {
        return false;
    }

    default PointerLikeCType asPointerLike() {
        throw new ClassCastException();
    }

    default boolean isPointer() {
        return false;
    }

    default PointerCType asPointer() {
        throw new ClassCastException();
    }

    default boolean isVoid() {
        return false;
    }

    default VoidCType asVoid() {
        throw new ClassCastException();
    }

    default boolean isWord() {
        return false;
    }

    default WordCType asWord() {
        throw new ClassCastException();
    }
}
