package cc.quarkus.qcc.graph.type;

public class ThrowValue implements Value<ThrowType> {

    @Override
    public ThrowType getType() {
        return ThrowType.INSTANCE;
    }
}
