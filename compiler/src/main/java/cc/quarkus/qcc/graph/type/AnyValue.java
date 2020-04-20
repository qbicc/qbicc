package cc.quarkus.qcc.graph.type;

public class AnyValue implements Value<AnyType, AnyValue> {
    @Override
    public AnyType getType() {
        return AnyType.INSTANCE;
    }
}
