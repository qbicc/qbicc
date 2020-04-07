package cc.quarkus.qcc.graph.type;

public class NullType implements ConcreteType<Object> {

    public static final NullType INSTANCE = new NullType();

    private NullType() {

    }
}
