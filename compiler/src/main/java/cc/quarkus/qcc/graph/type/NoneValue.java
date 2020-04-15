package cc.quarkus.qcc.graph.type;

public class NoneValue implements Value<NoneType> {

    @Override
    public NoneType getType() {
        return NoneType.INSTANCE;
    }
}
