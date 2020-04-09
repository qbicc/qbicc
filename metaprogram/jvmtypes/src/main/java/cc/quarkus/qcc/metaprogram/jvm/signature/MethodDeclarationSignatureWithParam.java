package cc.quarkus.qcc.metaprogram.jvm.signature;

final class MethodDeclarationSignatureWithParam implements MethodDeclarationSignature {
    private final MethodDeclarationSignature delegate;
    private final TypeSignature paramSig;
    private final int index;

    MethodDeclarationSignatureWithParam(final MethodDeclarationSignature delegate, final TypeSignature paramSig) {
        this.delegate = delegate;
        this.paramSig = paramSig;
        index = delegate.getParameterCount();
    }

    public int getTypeParameterCount() {
        return delegate.getTypeParameterCount();
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        return delegate.getTypeParameter(index);
    }

    public int getParameterCount() {
        return index + 1;
    }

    public TypeSignature getParameterType(final int index) throws IndexOutOfBoundsException {
        return index == this.index ? paramSig : delegate.getParameterType(index);
    }

    public boolean hasReturnType() {
        return delegate.hasReturnType();
    }

    public TypeSignature getReturnType() throws IllegalArgumentException {
        return delegate.getReturnType();
    }

    public int getThrowsCount() {
        return delegate.getThrowsCount();
    }

    public ThrowableTypeSignature getThrowsType(final int index) {
        return delegate.getThrowsType(index);
    }
}
