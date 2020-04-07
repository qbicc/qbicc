package cc.quarkus.qcc.graph.type;

public class AnyType implements ConcreteType<Object> {

    public static final AnyType INSTANCE = new AnyType();

    private AnyType() {

    }

    public Type join(Type other) {
        return other;
    }
}
