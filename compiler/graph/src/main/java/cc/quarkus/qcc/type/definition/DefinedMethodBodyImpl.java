package cc.quarkus.qcc.type.definition;

final class DefinedMethodBodyImpl implements DefinedMethodBody {
    private final DefinedMethodDefinitionImpl enclosing;
    private volatile VerifiedMethodBody verified;

    DefinedMethodBodyImpl(final DefinedMethodDefinitionImpl enclosing) {
        this.enclosing = enclosing;
    }

    public DefinedMethodDefinitionImpl getMethodDefinition() {
        return enclosing;
    }

    public VerifiedMethodBody verify() throws VerifyFailedException {
        VerifiedMethodBody verified = this.verified;
        if (verified != null) {
            return verified;
        }
        // todo: verify according to JVMS § 4.10
        synchronized (this) {
            verified = this.verified;
            if (verified == null) {
                verified = this.verified = new VerifiedMethodBodyImpl(this);
            }
            return verified;
        }
    }
}
