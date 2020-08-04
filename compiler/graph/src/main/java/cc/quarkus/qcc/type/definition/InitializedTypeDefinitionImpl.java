package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

final class InitializedTypeDefinitionImpl implements InitializedTypeDefinition {
    private final PreparedTypeDefinitionImpl delegate;

    InitializedTypeDefinitionImpl(final PreparedTypeDefinitionImpl delegate) {
        this.delegate = delegate;
    }

    public ClassType getClassType() {
        return delegate.getClassType();
    }

    public InitializedTypeDefinition getSuperClass() {
        return delegate.getSuperClass().initialize();
    }

    public InitializedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index).initialize();
    }

    public boolean isArray() {
        return delegate.isArray();
    }

    public Dictionary getDefiningClassLoader() {
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

    public ResolvedFieldDefinition resolveField(final Type type, final String name) {
        return delegate.resolveField(type, name);
    }

    public ResolvedFieldDefinition findField(final String name) {
        return delegate.findField(name);
    }

    public ResolvedFieldDefinition getFieldDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getFieldDefinition(index);
    }

    public ResolvedMethodDefinition getMethodDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getMethodDefinition(index);
    }

    public ResolvedMethodDefinition resolveMethod(final MethodIdentifier identifier) {
        return delegate.resolveMethod(identifier);
    }

    public ResolvedMethodDefinition resolveInterfaceMethod(final MethodIdentifier identifier) {
        return delegate.resolveInterfaceMethod(identifier);
    }

    public ResolvedMethodDefinition resolveInterfaceMethod(final MethodIdentifier identifier, final boolean searchingSuper) {
        return delegate.resolveInterfaceMethod(identifier, searchingSuper);
    }
}
