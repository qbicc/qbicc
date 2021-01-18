package cc.quarkus.qcc.type.definition;

/**
 *
 */
final class PreparedTypeDefinitionImpl extends DelegatingResolvedTypeDefinition implements PreparedTypeDefinition {
    private final ResolvedTypeDefinitionImpl delegate;
    private final FieldContainer staticFields;
    private volatile PreparedTypeDefinition initialized;
    private PreparedTypeDefinition initializing;

    PreparedTypeDefinitionImpl(final ResolvedTypeDefinitionImpl delegate) {
        this.delegate = delegate;
        staticFields = FieldContainer.forStaticFieldsOf(delegate);
    }

    // delegations

    public ResolvedTypeDefinition getDelegate() {
        return delegate;
    }

    public PreparedTypeDefinition validate() {
        return this;
    }

    public PreparedTypeDefinition resolve() {
        return this;
    }

    public PreparedTypeDefinition getSuperClass() {
        return super.getSuperClass().prepare();
    }

    public PreparedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return super.getInterface(index).prepare();
    }

    public FieldContainer getStaticFields() {
        return staticFields;
    }

    public InitializedTypeDefinition initialize() throws InitializationFailedException {
        throw new UnsupportedOperationException("To be removed");
    }
}
