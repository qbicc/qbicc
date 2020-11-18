package cc.quarkus.qcc.type.generic;

/**
 *
 */
final class TypeParameterWithInterfaceBound implements TypeParameter {
    private final TypeParameter delegate;
    private final ReferenceTypeSignature interfaceBound;
    private final int index;

    TypeParameterWithInterfaceBound(final TypeParameter delegate, final ReferenceTypeSignature interfaceBound) {
        this.delegate = delegate;
        this.interfaceBound = interfaceBound;
        index = delegate.getInterfaceBoundCount();
    }

    public String getSimpleName() {
        return delegate.getSimpleName();
    }

    public boolean hasClassBound() {
        return delegate.hasClassBound();
    }

    public ReferenceTypeSignature getClassBound() throws IllegalArgumentException {
        return delegate.getClassBound();
    }

    public int getInterfaceBoundCount() {
        return index + 1;
    }

    public ReferenceTypeSignature getInterfaceBound(final int index) throws IndexOutOfBoundsException {
        return index == this.index ? interfaceBound : delegate.getInterfaceBound(index);
    }
}
