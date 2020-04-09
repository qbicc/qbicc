package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 * The declaration signature of {@code Object} or other oddball root classes.
 */
final class RootClassDeclarationSignature implements ClassDeclarationSignature {
    private RootClassDeclarationSignature() {}

    static final RootClassDeclarationSignature INSTANCE = new RootClassDeclarationSignature();

    public int getTypeParameterCount() {
        return 0;
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public boolean hasSuperclass() {
        return false;
    }

    public ClassTypeSignature getSuperclass() {
        throw new IllegalArgumentException();
    }

    public int getInterfaceCount() {
        return 0;
    }

    public ClassTypeSignature getInterface(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }
}
