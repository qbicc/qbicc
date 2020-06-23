package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.universe.Universe;

final class DefinedMethodDefinitionImpl implements DefinedMethodDefinition {
    private final DefinedTypeDefinitionImpl enclosing;
    private final int index;
    private final DefinedMethodBody body;
    private final int modifiers;
    private final String name;
    private final String[] parameterDescriptors;
    private final String[] parameterNames;
    private final String returnTypeDescriptor;
    private volatile ResolvedMethodDefinitionImpl resolved;

    DefinedMethodDefinitionImpl(final DefinedTypeDefinitionImpl enclosing, final int index, final boolean body, final int modifiers, final String name, final String[] parameterDescriptors, final String returnTypeDescriptor, final String[] parameterNames) {
        this.enclosing = enclosing;
        this.index = index; // todo: remove
        this.body = body ? new DefinedMethodBodyImpl(this) : null;
        this.modifiers = modifiers;
        this.name = name;
        this.parameterNames = parameterNames;
        this.parameterDescriptors = parameterDescriptors;
        this.returnTypeDescriptor = returnTypeDescriptor;
    }

    public String getName() {
        return name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public int getParameterCount() {
        return parameterDescriptors.length;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        return parameterNames[index];
    }

    public DefinedTypeDefinitionImpl getEnclosingTypeDefinition() {
        return enclosing;
    }

    public ResolvedMethodDefinitionImpl resolve() throws ResolutionFailedException {
        ResolvedMethodDefinitionImpl resolved = this.resolved;
        if (resolved != null) {
            return resolved;
        }
        int len = parameterDescriptors.length;
        Universe classLoader = getEnclosingTypeDefinition().getDefiningClassLoader();
        Type[] paramTypes;
        Type returnType;
        try {
            paramTypes = new Type[len];
            for (int i = 0; i < len; i ++) {
                paramTypes[i] = classLoader.parseSingleDescriptor(parameterDescriptors[i]);
            }
            returnType = classLoader.parseSingleDescriptor(returnTypeDescriptor);
        } catch (Exception e) {
            throw new ResolutionFailedException(e);
        }
        synchronized (this) {
            resolved = this.resolved;
            if (resolved != null) {
                return resolved;
            }
            resolved = this.resolved = new ResolvedMethodDefinitionImpl(this, paramTypes, returnType);
        }
        return resolved;
    }

    public boolean hasMethodBody() {
        return body != null;
    }

    public DefinedMethodBody getMethodBody() {
        DefinedMethodBody body = this.body;
        if (body == null) {
            throw new IllegalArgumentException("No method body");
        }
        return body;
    }

    int getIndex() {
        return index;
    }
}
