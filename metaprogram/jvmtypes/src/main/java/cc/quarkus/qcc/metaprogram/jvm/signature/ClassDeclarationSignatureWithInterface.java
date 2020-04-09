package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class ClassDeclarationSignatureWithInterface implements ClassDeclarationSignature {
    private final ClassDeclarationSignature delegate;
    private final ClassTypeSignature interfaceSig;
    private final int index;

    ClassDeclarationSignatureWithInterface(final ClassDeclarationSignature delegate, final ClassTypeSignature interfaceSig) {
        this.delegate = delegate;
        this.interfaceSig = interfaceSig;
        index = delegate.getInterfaceCount();
    }

    public int getTypeParameterCount() {
        return delegate.getTypeParameterCount();
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        return delegate.getTypeParameter(index);
    }

    public boolean hasSuperclass() {
        return delegate.hasSuperclass();
    }

    public ClassTypeSignature getSuperclass() {
        return delegate.getSuperclass();
    }

    public int getInterfaceCount() {
        return index + 1;
    }

    public ClassTypeSignature getInterface(final int index) throws IndexOutOfBoundsException {
        return index == this.index ? interfaceSig : delegate.getInterface(index);
    }
}
