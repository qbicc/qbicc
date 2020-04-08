package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 *
 */
final class ClassTypeSignatureWithArgs implements ClassTypeSignature {
    final ClassTypeSignature delegate;
    final TypeArgument typeArgument;

    ClassTypeSignatureWithArgs(final ClassTypeSignature delegate, final TypeArgument typeArgument) {
        this.delegate = delegate;
        this.typeArgument = typeArgument;
    }

    public String getSimpleName() {
        return delegate.getSimpleName();
    }

    public boolean hasPackageName() {
        return delegate.hasPackageName();
    }

    public PackageName getPackageName() throws IllegalArgumentException {
        return delegate.getPackageName();
    }

    public boolean hasEnclosing() {
        return delegate.hasEnclosing();
    }

    public ClassTypeSignature getEnclosing() throws IllegalArgumentException {
        return delegate.getEnclosing();
    }

    public int getTypeArgumentCount() {
        return delegate.getTypeArgumentCount() + 1;
    }

    public TypeArgument getTypeArgument(final int index) throws IndexOutOfBoundsException {
        if (index == delegate.getTypeArgumentCount()) {
            return typeArgument;
        } else {
            return delegate.getTypeArgument(index);
        }
    }
}
