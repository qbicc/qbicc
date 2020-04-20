package cc.quarkus.qcc.graph.type;

public class FunctionType<T extends Type<T>> implements Type<T> {

    public FunctionType(T returnType) {
        this.returnType = returnType;
    }

    public T getReturnType() {
        return this.returnType;
    }

    private final T returnType;

}
