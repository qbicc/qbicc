package cc.quarkus.qcc.graph.type;

public class BooleanType implements ConcreteType<Boolean> {
    public static final BooleanType INSTANCE = new BooleanType();

    private BooleanType() {

    }
}
