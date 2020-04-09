package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class BaseTypeParameter implements TypeParameter {
    private final String simpleName;

    BaseTypeParameter(final String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public boolean hasClassBound() {
        return false;
    }

    public ReferenceTypeSignature getClassBound() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    public int getInterfaceBoundCount() {
        return 0;
    }

    public ReferenceTypeSignature getInterfaceBound(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }
}
