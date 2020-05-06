package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.Node;

public class CompletionToken {

    public CompletionToken(Object returnValue, ObjectReference throwValue) {
        this.returnValue = returnValue;
        this.throwValue = throwValue;
    }

    public Object returnValue() {
        return this.returnValue;
    }

    public ObjectReference throwValue() {
        return this.throwValue;
    }

    private final Object returnValue;

    private final ObjectReference throwValue;
}
