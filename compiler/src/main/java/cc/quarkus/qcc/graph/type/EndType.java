package cc.quarkus.qcc.graph.type;

public class EndType<T extends ConcreteType<?>> implements Type {
    public EndType(T returnType) {
        this.returnType = returnType;
    }

    private final T returnType;
}
