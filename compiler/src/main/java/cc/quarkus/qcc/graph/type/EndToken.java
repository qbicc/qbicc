package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.CallResult;
import cc.quarkus.qcc.type.ObjectReference;

public class EndToken<V> implements CallResult<V> {

    public EndToken(IOToken io, MemoryToken memory, V returnValue, ObjectReference throwValue) {
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

    public V getReturnValue() {
        return this.returnValue;
    }

    @Override
    public ObjectReference getThrowValue() {
        return this.throwValue;
    }

    private final IOToken io;

    private final MemoryToken memory;

    private final V returnValue;

    private final ObjectReference throwValue;
}
