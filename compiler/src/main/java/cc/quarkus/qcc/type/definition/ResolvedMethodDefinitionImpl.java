package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;

final class ResolvedMethodDefinitionImpl implements ResolvedMethodDefinition {
    private final DefinedMethodDefinitionImpl delegate;
    private final MethodIdentifier methodIdentifier;

    ResolvedMethodDefinitionImpl(final DefinedMethodDefinitionImpl delegate, final Type[] paramTypes, final Type returnType) {
        this.delegate = delegate;
        methodIdentifier = MethodIdentifier.of(getName(), MethodTypeDescriptor.of(returnType, paramTypes));
    }

    public String getName() {
        return delegate.getName();
    }

    public int getModifiers() {
        return delegate.getModifiers();
    }

    public int getParameterCount() {
        return delegate.getParameterCount();
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        return delegate.getParameterName(index);
    }

    public DefinedTypeDefinition getEnclosingTypeDefinition() {
        return delegate.getEnclosingTypeDefinition();
    }

    public boolean hasMethodBody() {
        return delegate.hasMethodBody();
    }

    public DefinedMethodBody getMethodBody() {
        return delegate.getMethodBody();
    }

    public MethodIdentifier getMethodIdentifier() {
        return methodIdentifier;
    }

    public Type getParameterType(final int index) throws IndexOutOfBoundsException {
        return getMethodIdentifier().getParameterType(index);
    }

    public Type getReturnType() {
        return getMethodIdentifier().getReturnType();
    }
}
