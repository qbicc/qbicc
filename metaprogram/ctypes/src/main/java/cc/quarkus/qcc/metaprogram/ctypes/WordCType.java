package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface WordCType extends ObjectCType {
    boolean isSigned();

    boolean isUnsigned();
}
