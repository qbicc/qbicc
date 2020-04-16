package cc.quarkus.qcc.graph.type;

public class EndValue implements Value<EndType<?>> {

    public EndValue(EndType<?> type, IOValue io, MemoryValue memory, Value<?> returnValue) {
        this.type = type;
        this.io = io;
        this.memory = memory;
        this.returnValue = returnValue;
    }

    @Override
    public EndType<?> getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "EndValue{" +
                "type=" + type +
                ", io=" + io +
                ", memory=" + memory +
                ", returnValue=" + returnValue +
                '}';
    }

    private final EndType<?> type;

    private final IOValue io;

    private final MemoryValue memory;

    private final Value<?> returnValue;
}
