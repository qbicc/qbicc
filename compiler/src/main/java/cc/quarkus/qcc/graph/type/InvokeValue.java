package cc.quarkus.qcc.graph.type;

public class InvokeValue implements Value<InvokeType>, IOSource, MemorySource {

    public InvokeValue(InvokeType type) {
        this.type = type;
        this.io = new IOValue();
        this.memory = new MemoryValue();
    }

    @Override
    public InvokeType getType() {
        return this.type;
    }

    @Override
    public IOValue getIO() {
        return this.io;
    }

    @Override
    public MemoryValue getMemory() {
        return this.memory;
    }

    private final InvokeType type;

    private final IOValue io;

    private final MemoryValue memory;
}
