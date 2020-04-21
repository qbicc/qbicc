package cc.quarkus.qcc.graph.type;

public class StartValue implements IOSource, MemorySource {

    public StartValue(Object...arguments) {
        this.arguments = arguments;
        this.io = new IOToken();
        this.memory = new MemoryToken();
    }

    public Object getArgument(int index) {
        return this.arguments[index];
    }

    @Override
    public IOToken getIO() {
        return this.io;
    }

    @Override
    public MemoryToken getMemory() {
        return this.memory;
    }

    private final Object[] arguments;
    private final IOToken io;
    private final MemoryToken memory;
}
