package cc.quarkus.qcc.graph.type;

public class AnyType implements ConcreteType<AnyValue> {

    public static final AnyType INSTANCE = new AnyType();

    private AnyType() {

    }

    public Type join(Type other) {
        return other;
    }
}
