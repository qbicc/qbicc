package cc.quarkus.qcc.type.generic;

/**
 * A Java reference type of some sort (JVM 14 spec §4.7.9).
 */
public interface ReferenceTypeSignature extends TypeSignature {
    default boolean isReference() {
        return true;
    }

    default ReferenceTypeSignature asReference() {
        return this;
    }
}
