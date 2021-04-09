package org.qbicc.type.definition;

/**
 *
 */
final class VerificationFailedDefinitionImpl extends DelegatingDefinedTypeDefinition {
    private final DefinedTypeDefinition delegate;
    private final String msg;

    private final Throwable cause;

    VerificationFailedDefinitionImpl(final DefinedTypeDefinition delegate, final String msg, final Throwable cause) {
        this.delegate = delegate;
        this.msg = msg;
        this.cause = cause;
    }

    protected DefinedTypeDefinition getDelegate() {
        return delegate;
    }

    public ValidatedTypeDefinition validate() {
        throw new VerifyFailedException(msg, cause);
    }
}
