package cc.quarkus.qcc.graph.type;

public class MemoryValue implements Value<MemoryType> {

    @Override
    public MemoryType getType() {
        return MemoryType.INSTANCE;
    }
}
