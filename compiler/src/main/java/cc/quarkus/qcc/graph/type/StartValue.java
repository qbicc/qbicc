package cc.quarkus.qcc.graph.type;

public class StartValue implements Value<StartType, StartValue>, IOSource, MemorySource {

    public StartValue(StartType type, Value<? extends ConcreteType<?>,?>...arguments) {
        this.type = type;
        this.arguments = arguments;
        this.io = new IOValue();
        this.memory = new MemoryValue();
    }

    @Override
    public StartType getType() {
        return this.type;
    }

    public Value<? extends ConcreteType<?>,?> getArgument(int index) {
        return this.arguments[index];
    }

    @Override
    public IOValue getIO() {
        return this.io;
    }

    @Override
    public MemoryValue getMemory() {
        return this.memory;
    }

    private final StartType type;
    private final Value<? extends ConcreteType<?>,?>[] arguments;
    private final IOValue io;
    private final MemoryValue memory;
}
