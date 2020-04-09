package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class ClassDeclarationSignatureWithSuperclass implements ClassDeclarationSignature {
    private final ClassDeclarationSignature delegate;
    private final ClassTypeSignature superclass;

    ClassDeclarationSignatureWithSuperclass(final ClassDeclarationSignature delegate, final ClassTypeSignature superclass) {
        this.delegate = delegate;
        this.superclass = superclass;
    }

    public int getTypeParameterCount() {
        return delegate.getTypeParameterCount();
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        return delegate.getTypeParameter(index);
    }

    public boolean hasSuperclass() {
        return true;
    }

    public ClassTypeSignature getSuperclass() {
        return superclass;
    }

    public int getInterfaceCount() {
        return delegate.getInterfaceCount();
    }

    public ClassTypeSignature getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index);
    }
}
