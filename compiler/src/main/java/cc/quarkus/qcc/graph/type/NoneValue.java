package cc.quarkus.qcc.graph.type;

public class NoneValue implements Value<NoneType, NoneValue> {

    @Override
    public NoneType getType() {
        return NoneType.INSTANCE;
    }
}
