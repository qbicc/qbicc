package cc.quarkus.qcc.graph.type;

public class NullValue implements Value<NullType, NullValue> {

    public static final NullValue NULL = new NullValue();

    private NullValue() {
    }

    @Override
    public NullType getType() {
        return NullType.INSTANCE;
    }
}
