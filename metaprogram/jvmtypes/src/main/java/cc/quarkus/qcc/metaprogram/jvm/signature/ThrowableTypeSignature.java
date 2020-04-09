package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
public interface ThrowableTypeSignature extends ReferenceTypeSignature {
    default boolean isThrowable() {
        return true;
    }

    default ThrowableTypeSignature asThrowable() {
        return this;
    }
}
