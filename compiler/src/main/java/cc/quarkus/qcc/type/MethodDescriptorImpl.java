package cc.quarkus.qcc.type;

import java.util.List;

public class MethodDescriptorImpl implements MethodDescriptor {

    MethodDescriptorImpl(TypeDefinition owner, String name, List<TypeDescriptor<?>> paramTypes, TypeDescriptor<?> returnType, String descriptor, boolean isStatic) {
        this.owner = owner;
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.descriptor = descriptor;
        this.isStatic = isStatic;
    }

    @Override
    public TypeDefinition getOwner() {
        return this.owner;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isStatic() {
        return this.isStatic;
    }

    @Override
    public List<TypeDescriptor<?>> getParamTypes() {
        return this.paramTypes;
    }

    @Override
    public TypeDescriptor<?> getReturnType() {
        return this.returnType;
    }

    @Override
    public String getDescriptor() {
        return this.descriptor;
    }

    private final List<TypeDescriptor<?>> paramTypes;

    private final TypeDescriptor<?> returnType;

    private final TypeDefinition owner;

    private final String name;

    private final boolean isStatic;

    private final String descriptor;
}
