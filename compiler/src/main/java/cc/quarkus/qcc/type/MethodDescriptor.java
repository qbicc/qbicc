package cc.quarkus.qcc.type;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class MethodDescriptor {

    MethodDescriptor(List<ConcreteType<?>> paramTypes, ConcreteType<?> returnType) {
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    public List<ConcreteType<?>> getParamTypes() {
        return this.paramTypes;
    }

    public ConcreteType<?> getReturnType() {
        return this.returnType;
    }

    private final List<ConcreteType<?>> paramTypes;

    private final ConcreteType<?> returnType;
}
