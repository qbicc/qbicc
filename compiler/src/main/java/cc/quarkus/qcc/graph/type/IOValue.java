package cc.quarkus.qcc.graph.type;

public class IOValue implements Value<IOType> {
    @Override
    public IOType getType() {
        return IOType.INSTANCE;
    }
}
