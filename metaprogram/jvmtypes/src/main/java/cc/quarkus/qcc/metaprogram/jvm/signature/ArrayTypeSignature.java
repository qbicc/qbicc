package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
public interface ArrayTypeSignature extends ReferenceTypeSignature {
    /**
     * Get the signature of the array's member type.
     *
     * @return the signature of the array's member type
     */
    TypeSignature getMemberSignature();
}
