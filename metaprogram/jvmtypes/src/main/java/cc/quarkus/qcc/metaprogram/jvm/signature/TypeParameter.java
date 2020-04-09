package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 * A formal type parameter. For actual type parameter arguments, see {@link TypeArgument}.
 */
public interface TypeParameter {
    String getSimpleName();

    boolean hasClassBound();

    /**
     * Get the class bound for this type parameter declaration.  Top level bounds are always covariant.
     *
     * @return the covariant bound
     * @throws IllegalArgumentException if there is no bound
     */
    ReferenceTypeSignature getClassBound() throws IllegalArgumentException;

    int getInterfaceBoundCount();

    ReferenceTypeSignature getInterfaceBound(int index) throws IndexOutOfBoundsException;
}
