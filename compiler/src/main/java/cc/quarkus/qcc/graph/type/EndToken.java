package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.CallResult;

public class EndToken implements CallResult {

    public EndToken(IOToken io, MemoryToken memory, Object returnValue, ObjectReference throwValue) {
        this.io = io;
        this.memory = memory;
        this.returnValue = returnValue;
        this.throwValue = throwValue;
    }

    @Override
    public String toString() {
        return "EndToken{" +
                "io=" + io +
                ", memory=" + memory +
                ", returnValue=" + returnValue +
                ", throwValue=" + throwValue +
                '}';
    }

    public Object getReturnValue() {
        return this.returnValue;
    }

    @Override
    public ObjectReference getThrowValue() {
        return this.throwValue;
    }

    private final IOToken io;

    private final MemoryToken memory;

    private final Object returnValue;

    private final ObjectReference throwValue;
}
