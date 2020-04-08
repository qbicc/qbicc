package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 * A type variable signature.
 */
public interface TypeVariableSignature extends ReferenceTypeSignature {
    String getSimpleName();
}
