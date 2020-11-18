package cc.quarkus.qcc.type.generic;

/**
 * A type variable signature.
 */
public interface TypeVariableSignature extends ThrowableTypeSignature {
    default boolean isTypeVariable() {
        return true;
    }

    default TypeVariableSignature asTypeVariable() {
        return this;
    }

    String getSimpleName();
}
