package cc.quarkus.qcc.type.generic;

final class MethodDeclarationSignatureWithThrows implements MethodDeclarationSignature {
    private final MethodDeclarationSignature delegate;
    private final ThrowableTypeSignature throwable;
    private final int index;

    MethodDeclarationSignatureWithThrows(final MethodDeclarationSignature delegate, final ThrowableTypeSignature throwable) {
        this.delegate = delegate;
        this.throwable = throwable;
        index = delegate.getThrowsCount();
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

    public boolean hasReturnType() {
        return delegate.hasReturnType();
    }

    public TypeSignature getReturnType() throws IllegalArgumentException {
        return delegate.getReturnType();
    }

    public int getThrowsCount() {
        return index + 1;
    }

    public ThrowableTypeSignature getThrowsType(final int index) {
        return index == this.index ? throwable : delegate.getThrowsType(index);
    }
}
