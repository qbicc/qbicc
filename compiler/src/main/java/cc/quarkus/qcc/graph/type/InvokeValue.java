package cc.quarkus.qcc.graph.type;

public class InvokeValue implements IOSource, MemorySource {

    public InvokeValue(Object returnValue, ObjectReference throwValue) {
        this.io = new IOToken();
        this.memory = new MemoryToken();
        this.returnValue = returnValue;
        this.throwValue = throwValue;
    }

    @Override
    public IOToken getIO() {
        return this.io;
    }

    @Override
    public MemoryToken getMemory() {
        return this.memory;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public ObjectReference getThrowValue() {
        return this.throwValue;
    }

    private final IOToken io;

    private final MemoryToken memory;
    private final Object returnValue;
    private final ObjectReference throwValue;
}
