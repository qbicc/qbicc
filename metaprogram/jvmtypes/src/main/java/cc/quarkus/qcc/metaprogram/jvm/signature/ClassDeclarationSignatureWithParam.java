package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class ClassDeclarationSignatureWithParam implements ClassDeclarationSignature {
    private final ClassDeclarationSignature delegate;
    private final TypeParameter typeParameter;
    private final int index;

    ClassDeclarationSignatureWithParam(final ClassDeclarationSignature delegate, final TypeParameter typeParameter) {
        this.delegate = delegate;
        this.typeParameter = typeParameter;
        index = delegate.getTypeParameterCount();
    }

    public int getTypeParameterCount() {
        return index + 1;
    }

    public TypeParameter getTypeParameter(final int index) throws IndexOutOfBoundsException {
        return index == this.index ? typeParameter : delegate.getTypeParameter(index);
    }

    public boolean hasSuperclass() {
        return delegate.hasSuperclass();
    }

    public ClassTypeSignature getSuperclass() {
        return delegate.getSuperclass();
    }

    public int getInterfaceCount() {
        return delegate.getInterfaceCount();
    }

    public ClassTypeSignature getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index);
    }
}
