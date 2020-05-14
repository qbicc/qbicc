package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QType;

public class CompletionToken<V> implements QType {

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
