package cc.quarkus.qcc.graph.type;

public class MemoryValue implements Value<MemoryType,MemoryValue> {

    @Override
    public MemoryType getType() {
        return MemoryType.INSTANCE;
    }
}
