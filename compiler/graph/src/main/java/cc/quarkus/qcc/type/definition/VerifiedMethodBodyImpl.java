package cc.quarkus.qcc.type.definition;

final class VerifiedMethodBodyImpl implements VerifiedMethodBody {
    private final DefinedMethodBodyImpl delegate;
    private volatile ResolvedMethodBodyImpl resolved;

    VerifiedMethodBodyImpl(final DefinedMethodBodyImpl delegate) {
        this.delegate = delegate;
    }

    public DefinedMethodDefinitionImpl getMethodDefinition() {
        return delegate.getMethodDefinition();
    }

    public ResolvedMethodBodyImpl resolve() throws ResolutionFailedException {
        ResolvedMethodBodyImpl resolved = this.resolved;
        if (resolved != null) {
            return resolved;
        }
        synchronized (this) {
            resolved = this.resolved;
            if (resolved == null) {
                resolved = this.resolved = new ResolvedMethodBodyImpl(this);
            }
        }
        return resolved;
    }
}
