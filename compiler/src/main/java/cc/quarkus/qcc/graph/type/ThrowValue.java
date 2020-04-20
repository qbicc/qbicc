package cc.quarkus.qcc.graph.type;

public class ThrowValue implements Value<ThrowType, ThrowValue> {

    @Override
    public ThrowType getType() {
        return ThrowType.INSTANCE;
    }
}
