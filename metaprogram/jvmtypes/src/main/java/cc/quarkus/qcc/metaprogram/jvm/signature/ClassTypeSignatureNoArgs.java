package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.metaprogram.jvm.PackageName;
import io.smallrye.common.constraint.Assert;

class ClassTypeSignatureNoArgs implements ClassTypeSignature {
    private final PackageName packageName;
    private final ClassTypeSignature enclosing;
    private final String simpleName;

    ClassTypeSignatureNoArgs(final PackageName packageName, final ClassTypeSignature enclosing, final String simpleName) {
        assert packageName == null || enclosing == null;
        this.packageName = packageName;
        this.enclosing = enclosing;
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public boolean hasPackageName() {
        return packageName != null;
    }

    public PackageName getPackageName() throws IllegalArgumentException {
        Assert.checkNotNullParam("packageName", packageName);
        return packageName;
    }

    public boolean hasEnclosing() {
        return enclosing != null;
    }

    public ClassTypeSignature getEnclosing() throws IllegalArgumentException {
        Assert.checkNotNullParam("enclosing", enclosing);
        return enclosing;
    }

    public int getTypeArgumentCount() {
        return 0;
    }

    public TypeArgument getTypeArgument(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }
}
