package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.CallResult;

public class EndToken implements CallResult {

    public EndToken(IOToken io, MemoryToken memory, Object returnValue) {
        this.io = io;
        this.memory = memory;
        this.returnValue = returnValue;
    }

    @Override
    public String toString() {
        return "EndValue{" +
                ", io=" + io +
                ", memory=" + memory +
                ", returnValue=" + returnValue +
                '}';
    }

    public Object getReturnValue() {
        return this.returnValue;
    }

    private final IOToken io;

    private final MemoryToken memory;

    private final Object returnValue;
}
