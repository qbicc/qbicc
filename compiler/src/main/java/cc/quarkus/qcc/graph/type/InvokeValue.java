package cc.quarkus.qcc.graph.type;

public class InvokeValue implements Value<InvokeType,InvokeValue>, IOSource, MemorySource {

    public InvokeValue(InvokeType type, Value<? extends ConcreteType<?>,?> returnValue, ObjectValue throwValue) {
        this.type = type;
        this.io = new IOValue();
        this.memory = new MemoryValue();
        this.returnValue = returnValue;
        this.throwValue = throwValue;
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

    public Value<? extends ConcreteType<?>, ?> getReturnValue() {
        return this.returnValue;
    }

    public ObjectValue getThrowValue() {
        return this.throwValue;
    }

    private final InvokeType type;

    private final IOValue io;

    private final MemoryValue memory;
    private final Value<? extends ConcreteType<?>, ?> returnValue;
    private final ObjectValue throwValue;
}
