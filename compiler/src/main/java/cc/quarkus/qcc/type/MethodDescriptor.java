package cc.quarkus.qcc.type;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class MethodDescriptor {

    MethodDescriptor(TypeDefinition owner, String name, List<ConcreteType<?>> paramTypes, ConcreteType<?> returnType, boolean isStatic) {
        this.owner = owner;
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.isStatic = isStatic;
    }

    public TypeDefinition getOwner() {
        return this.owner;
    }

    public String getName() {
        return this.name;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public List<ConcreteType<?>> getParamTypes() {
        return this.paramTypes;
    }

    public ConcreteType<?> getReturnType() {
        return this.returnType;
    }

    private final List<ConcreteType<?>> paramTypes;

    private final ConcreteType<?> returnType;

    private final TypeDefinition owner;

    private final String name;

    private final boolean isStatic;
}
