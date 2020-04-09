package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.metaprogram.jvm.PackageName;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class PackageNameImpl implements PackageName {
    private final PackageName enclosing;
    private final String simpleName;

    PackageNameImpl(final String simpleName) {
        this.enclosing = null;
        this.simpleName = simpleName;
    }

    PackageNameImpl(final PackageName enclosing, final String simpleName) {
        this.enclosing = enclosing;
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public PackageName getEnclosing() {
        return Assert.checkNotNullParam("enclosing", enclosing);
    }

    public boolean hasEnclosing() {
        return enclosing != null;
    }
}
