package cc.quarkus.qcc.graph.type;

public class EndValue<T extends ConcreteType<T>> implements Value<EndType<T>, EndValue<T>> {

    public EndValue(EndType<T> type, IOValue io, MemoryValue memory, Value<T,?> returnValue) {
        this.type = type;
        this.io = io;
        this.memory = memory;
        this.returnValue = returnValue;
    }

    @Override
    public EndType<T> getType() {
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

    private final EndType<T> type;

    private final IOValue io;

    private final MemoryValue memory;

    private final Value<T,?> returnValue;
}
