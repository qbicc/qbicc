package cc.quarkus.qcc.type.descriptor;

import java.util.List;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public class MethodDescriptorImpl implements MethodDescriptor {

    MethodDescriptorImpl(TypeDefinition owner, String name, List<Type> paramTypes, Type returnType, String descriptor, boolean isStatic) {
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
    public List<Type> getParamTypes() {
        return this.paramTypes;
    }

    @Override
    public Type getReturnType() {
        return this.returnType;
    }

    @Override
    public String getDescriptor() {
        return this.descriptor;
    }

    @Override
    public String toString() {
        return "MethodDescriptorImpl{" +
                "paramTypes=" + paramTypes +
                ", returnType=" + returnType +
                ", owner=" + owner +
                ", name='" + name + '\'' +
                ", isStatic=" + isStatic +
                ", descriptor='" + descriptor + '\'' +
                '}';
    }

    private final List<Type> paramTypes;

    private final Type returnType;

    private final TypeDefinition owner;

    private final String name;

    private final boolean isStatic;

    private final String descriptor;
}
