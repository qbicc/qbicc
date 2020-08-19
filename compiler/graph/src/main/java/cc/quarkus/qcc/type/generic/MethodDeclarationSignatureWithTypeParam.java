package cc.quarkus.qcc.type.generic;

final class MethodDeclarationSignatureWithTypeParam implements MethodDeclarationSignature {
    private final MethodDeclarationSignature delegate;
    private final TypeParameter typeParam;
    private final int index;

    MethodDeclarationSignatureWithTypeParam(final MethodDeclarationSignature delegate, final TypeParameter typeParam) {
        this.delegate = delegate;
        this.typeParam = typeParam;
        index = delegate.getTypeParameterCount();
    }

    public int getTypeParameterCount() {
        return index + 1;
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        return index == this.index ? typeParam : delegate.getTypeParameter(index);
    }

    public int getParameterCount() {
        return delegate.getParameterCount();
    }

    public TypeSignature getParameterType(final int index) throws IndexOutOfBoundsException {
        return delegate.getParameterType(index);
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
