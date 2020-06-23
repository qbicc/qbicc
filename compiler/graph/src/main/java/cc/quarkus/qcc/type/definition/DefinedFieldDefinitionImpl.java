package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;

/**
 *
 */
final class DefinedFieldDefinitionImpl implements DefinedFieldDefinition {
    private final DefinedTypeDefinitionImpl enclosing;
    private final int modifiers;
    private final String name;
    private final String descriptor;
    private volatile ResolvedFieldDefinition resolved;

    DefinedFieldDefinitionImpl(final DefinedTypeDefinitionImpl enclosing, final int modifiers, final String name, final String descriptor) {
        this.enclosing = enclosing;
        this.modifiers = modifiers;
        this.name = name;
        this.descriptor = descriptor;
    }

    public String getName() {
        return name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public DefinedTypeDefinition getEnclosingTypeDefinition() {
        return enclosing;
    }

    public ResolvedFieldDefinition resolve() throws ResolutionFailedException {
        ResolvedFieldDefinition resolved = this.resolved;
        if (resolved != null) {
            return resolved;
        }
        Type type;
        try {
            type = enclosing.getDefiningClassLoader().parseSingleDescriptor(descriptor);
        } catch (VerifyFailedException e) {
            throw new ResolutionFailedException("Failed to resolve type descriptor " + descriptor + " for field " + name + " of  " + enclosing.getName());
        }
        synchronized (this) {
            resolved = this.resolved;
            if (resolved == null) {
                resolved = this.resolved = new ResolvedFieldDefinitionImpl(this, type);
            }
        }
        return resolved;
    }
}
