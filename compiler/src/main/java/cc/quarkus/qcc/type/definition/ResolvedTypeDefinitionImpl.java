package cc.quarkus.qcc.type.definition;

/**
 *
 */
final class ResolvedTypeDefinitionImpl extends DelegatingValidatedTypeDefinition implements ResolvedTypeDefinition {
    private final ValidatedTypeDefinition delegate;
    private volatile PreparedTypeDefinitionImpl prepared;

    ResolvedTypeDefinitionImpl(final ValidatedTypeDefinition delegate) {
        this.delegate = delegate;
    }

    public ValidatedTypeDefinition getDelegate() {
        return delegate;
    }

    public ResolvedTypeDefinition getSuperClass() {
        return delegate.getSuperClass().resolve();
    }

    public ResolvedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index).resolve();
    }

    public ResolvedTypeDefinitionImpl validate() {
        return this;
    }

    public ResolvedTypeDefinition resolve() {
        return this;
    }

    // next phase

    public PreparedTypeDefinitionImpl prepare() throws PrepareFailedException {
        PreparedTypeDefinitionImpl prepared = this.prepared;
        if (prepared != null) {
            return prepared;
        }
        ResolvedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.prepare();
        }
        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            getInterface(i).prepare();
        }
        synchronized (this) {
            prepared = this.prepared;
            if (prepared == null) {
                prepared = new PreparedTypeDefinitionImpl(this);
                this.prepared = prepared;
            }
        }
        return prepared;
    }
}
