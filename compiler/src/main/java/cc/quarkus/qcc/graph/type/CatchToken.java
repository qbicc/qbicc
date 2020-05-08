package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.ObjectReference;

public class CatchToken {
    public CatchToken(ObjectReference thrown) {
        this.thrown = thrown;
    }

    private final ObjectReference thrown;
}
