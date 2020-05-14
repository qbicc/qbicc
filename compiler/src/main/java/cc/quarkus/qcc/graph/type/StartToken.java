package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.QType;

public class StartToken implements QType, IOSource, MemorySource {

    public StartToken(QType...arguments) {
        this.arguments = arguments;
        this.io = new IOToken();
        this.memory = new MemoryToken();
    }

    public QType getArgument(int index) {
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

    private final QType[] arguments;
    private final IOToken io;
    private final MemoryToken memory;
}
