package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
public interface ArrayTypeSignature extends ReferenceTypeSignature {
    default boolean isArray() {
        return true;
    }

    default ArrayTypeSignature asArray() {
        return this;
    }

    /**
     * Get the signature of the array's member type.
     *
     * @return the signature of the array's member type
     */
    TypeSignature getMemberSignature();
}
