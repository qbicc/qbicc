package cc.quarkus.qcc.graph.type;

public class EndToken {

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

    private final IOToken io;

    private final MemoryToken memory;

    private final Object returnValue;
}
