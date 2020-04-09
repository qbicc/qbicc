package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 *
 */
final class ClassTypeSignatureWithArgs implements ClassTypeSignature {
    final int index;
    final ClassTypeSignature delegate;
    final TypeArgument typeArgument;

    ClassTypeSignatureWithArgs(final ClassTypeSignature delegate, final TypeArgument typeArgument) {
        index = delegate.getTypeArgumentCount();
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
        return index + 1;
    }

    public TypeArgument getTypeArgument(final int index) throws IndexOutOfBoundsException {
        if (index == this.index) {
            return typeArgument;
        } else {
            return delegate.getTypeArgument(index);
        }
    }

    private static final StackWalker W = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public StringBuilder toString(final StringBuilder b) {
        delegate.toString(b);
        if (index == 0) {
            b.append('<');
        } else {
            b.append(',');
        }
        typeArgument.toString(b);
        // this is not my best moment
        if (W.getCallerClass() != ClassTypeSignatureWithArgs.class) {
            b.append('>');
        }
        return b;
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public ClassTypeSignature getRawType() {
        return delegate.getRawType();
    }
}
