package cc.quarkus.qcc.type.definition;

/**
 *
 */
public abstract class DelegatingResolvedTypeDefinition extends DelegatingVerifiedTypeDefinition implements ResolvedTypeDefinition {
    DelegatingResolvedTypeDefinition() {}

    protected abstract ResolvedTypeDefinition getDelegate();

    public PreparedTypeDefinition prepare() throws PrepareFailedException {
        return getDelegate().prepare();
    }

    public ResolvedTypeDefinition verify() {
        return this;
    }

    public ResolvedTypeDefinition resolve() {
        return this;
    }

    public ResolvedTypeDefinition getSuperClass() {
        return getDelegate().getSuperClass();
    }

    public ResolvedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return getDelegate().getInterface(index);
    }
}
