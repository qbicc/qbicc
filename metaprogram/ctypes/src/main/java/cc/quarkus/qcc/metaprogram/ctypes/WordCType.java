package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface WordCType extends ObjectCType {
    default boolean isWord() {
        return true;
    }

    default WordCType asWord() {
        return this;
    }

    boolean isSigned();

    boolean isUnsigned();
}
