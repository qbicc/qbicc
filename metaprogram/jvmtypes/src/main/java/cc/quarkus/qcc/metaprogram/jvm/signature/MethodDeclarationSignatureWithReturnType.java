package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class MethodDeclarationSignatureWithReturnType implements MethodDeclarationSignature {
    private final MethodDeclarationSignature delegate;
    private final TypeSignature retSig;

    MethodDeclarationSignatureWithReturnType(final MethodDeclarationSignature delegate, final TypeSignature retSig) {
        this.delegate = delegate;
        this.retSig = retSig;
    }

    public int getTypeParameterCount() {
        return delegate.getTypeParameterCount();
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        return delegate.getTypeParameter(index);
    }

    public int getParameterCount() {
        return delegate.getParameterCount();
    }

    public TypeSignature getParameterType(final int index) throws IndexOutOfBoundsException {
        return delegate.getParameterType(index);
    }

    public int getThrowsCount() {
        return delegate.getThrowsCount();
    }

    public ThrowableTypeSignature getThrowsType(final int index) {
        return delegate.getThrowsType(index);
    }

    public boolean hasReturnType() {
        return true;
    }

    public TypeSignature getReturnType() throws IllegalArgumentException {
        return retSig;
    }
}
