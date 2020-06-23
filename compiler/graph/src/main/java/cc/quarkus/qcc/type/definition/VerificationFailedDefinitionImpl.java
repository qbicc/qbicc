package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.universe.Universe;

/**
 *
 */
final class VerificationFailedDefinitionImpl implements DefinedTypeDefinition {
    private final DefinedTypeDefinitionImpl delegate;
    private final String msg;
    private final Throwable cause;

    VerificationFailedDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final String msg, final Throwable cause) {
        this.delegate = delegate;
        this.msg = msg;
        this.cause = cause;
    }

    public Universe getDefiningClassLoader() {
        return delegate.getDefiningClassLoader();
    }

    public String getName() {
        return delegate.getName();
    }

    public int getModifiers() {
        return delegate.getModifiers();
    }

    public String getSuperClassName() {
        return delegate.getSuperClassName();
    }

    public int getInterfaceCount() {
        return delegate.getInterfaceCount();
    }

    public String getInterfaceName(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterfaceName(index);
    }

    public int getFieldCount() {
        return delegate.getFieldCount();
    }

    public int getMethodCount() {
        return delegate.getMethodCount();
    }

    public DefinedFieldDefinition getFieldDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getFieldDefinition(index);
    }

    public DefinedMethodDefinition getMethodDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getMethodDefinition(index);
    }

    public VerifiedTypeDefinition verify() {
        throw new VerifyFailedException(msg, cause);
    }
}
