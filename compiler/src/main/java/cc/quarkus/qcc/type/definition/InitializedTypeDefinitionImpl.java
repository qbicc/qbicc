package cc.quarkus.qcc.type.definition;

final class InitializedTypeDefinitionImpl extends DelegatingPreparedTypeDefinition implements InitializedTypeDefinition {
    private final PreparedTypeDefinitionImpl delegate;

    InitializedTypeDefinitionImpl(final PreparedTypeDefinitionImpl delegate) {
        this.delegate = delegate;
    }

    public PreparedTypeDefinition getDelegate() {
        return delegate;
    }

    public InitializedTypeDefinition getSuperClass() {
        return super.getSuperClass().initialize();
    }

    public InitializedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return super.getInterface(index).initialize();
    }

    public InitializedTypeDefinition prepare() {
        return this;
    }

    public InitializedTypeDefinition validate() {
        return this;
    }

    public InitializedTypeDefinition resolve() {
        return this;
    }
}
