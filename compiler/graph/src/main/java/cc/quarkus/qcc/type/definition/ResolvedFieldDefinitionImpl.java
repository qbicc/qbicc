package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;

/**
 *
 */
final class ResolvedFieldDefinitionImpl implements ResolvedFieldDefinition {
    private final DefinedFieldDefinitionImpl delegate;
    private final Type type;

    ResolvedFieldDefinitionImpl(final DefinedFieldDefinitionImpl delegate, final Type type) {
        this.delegate = delegate;
        this.type = type;
    }

    public ResolvedTypeDefinition getEnclosingTypeDefinition() {
        return delegate.getEnclosingTypeDefinition().verify().resolve();
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return delegate.getName();
    }

    public int getModifiers() {
        return delegate.getModifiers();
    }
}
