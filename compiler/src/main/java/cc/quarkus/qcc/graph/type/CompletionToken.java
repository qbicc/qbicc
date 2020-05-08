package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.ObjectReference;

public class CompletionToken<V> {

    public CompletionToken(V returnValue, ObjectReference throwValue) {
        this.returnValue = returnValue;
        this.throwValue = throwValue;
    }

    public V returnValue() {
        return this.returnValue;
    }

    public ObjectReference throwValue() {
        return this.throwValue;
    }

    private final V returnValue;

    private final ObjectReference throwValue;
}
