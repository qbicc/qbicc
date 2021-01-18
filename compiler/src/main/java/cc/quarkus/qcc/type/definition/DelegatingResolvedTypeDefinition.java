package cc.quarkus.qcc.type.definition;

/**
 *
 */
public abstract class DelegatingResolvedTypeDefinition extends DelegatingValidatedTypeDefinition implements ResolvedTypeDefinition {
    DelegatingResolvedTypeDefinition() {}

    protected abstract ResolvedTypeDefinition getDelegate();

    public ResolvedTypeDefinition validate() {
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

    public FieldSet getInstanceFieldSet() {
        return getDelegate().getInstanceFieldSet();
    }

    public FieldSet getStaticFieldSet() {
        return getDelegate().getStaticFieldSet();
    }
}
