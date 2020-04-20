package cc.quarkus.qcc.graph.type;

public class IOValue implements Value<IOType, IOValue> {
    @Override
    public IOType getType() {
        return IOType.INSTANCE;
    }
}
