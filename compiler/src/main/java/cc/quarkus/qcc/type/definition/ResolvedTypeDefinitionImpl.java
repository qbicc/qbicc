package cc.quarkus.qcc.type.definition;

/**
 *
 */
final class ResolvedTypeDefinitionImpl extends DelegatingValidatedTypeDefinition implements ResolvedTypeDefinition {
    private final ValidatedTypeDefinition delegate;

    ResolvedTypeDefinitionImpl(final ValidatedTypeDefinition delegate) {
        this.delegate = delegate;
    }

    public ValidatedTypeDefinition getDelegate() {
        return delegate;
    }

    public ResolvedTypeDefinition getSuperClass() {
        ValidatedTypeDefinition superClass = delegate.getSuperClass();
        return superClass == null ? null : superClass.resolve();
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

    public int getTypeId() {
        return delegate.getTypeId();
    }

    public boolean isTypeIdValid() {
        return delegate.isTypeIdValid();
    }

    public void assignTypeId(int myTypeId) {
        delegate.assignTypeId(myTypeId);
    }
}
