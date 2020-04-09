package cc.quarkus.qcc.metaprogram.jvm.signature;

final class RootMethodDeclarationSignature implements MethodDeclarationSignature {
    static final RootMethodDeclarationSignature INSTANCE = new RootMethodDeclarationSignature();

    private RootMethodDeclarationSignature() {}

    public int getTypeParameterCount() {
        return 0;
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public int getParameterCount() {
        return 0;
    }

    public TypeSignature getParameterType(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public boolean hasReturnType() {
        return false;
    }

    public TypeSignature getReturnType() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    public int getThrowsCount() {
        return 0;
    }

    public ThrowableTypeSignature getThrowsType(final int index) {
        throw new IndexOutOfBoundsException(index);
    }
}
