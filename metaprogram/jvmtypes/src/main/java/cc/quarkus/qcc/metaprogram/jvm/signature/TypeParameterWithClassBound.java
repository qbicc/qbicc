package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class TypeParameterWithClassBound implements TypeParameter {
    private final TypeParameter delegate;
    private final ReferenceTypeSignature classBound;

    TypeParameterWithClassBound(final TypeParameter delegate, final ReferenceTypeSignature classBound) {
        this.delegate = delegate;
        this.classBound = classBound;
    }

    public String getSimpleName() {
        return delegate.getSimpleName();
    }

    public boolean hasClassBound() {
        return true;
    }

    public ReferenceTypeSignature getClassBound() throws IllegalArgumentException {
        return classBound;
    }

    public int getInterfaceBoundCount() {
        return delegate.getInterfaceBoundCount();
    }

    public ReferenceTypeSignature getInterfaceBound(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterfaceBound(index);
    }
}
