package cc.quarkus.qcc.type.definition;

/**
 *
 */
final class InitializationFailedDefinitionImpl extends DelegatingPreparedTypeDefinition {
    private final PreparedTypeDefinitionImpl delegate;
    private final InitializationFailedException e;

    InitializationFailedDefinitionImpl(final PreparedTypeDefinitionImpl delegate, final InitializationFailedException e) {
        this.delegate = delegate;
        this.e = e;
    }

    protected PreparedTypeDefinition getDelegate() {
        return delegate;
    }

    public InitializedTypeDefinition initialize() throws InitializationFailedException {
        throw e;
    }
}
