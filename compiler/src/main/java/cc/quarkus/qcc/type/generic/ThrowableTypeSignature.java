package cc.quarkus.qcc.type.generic;

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
