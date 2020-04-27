package cc.quarkus.qcc.graph.type;

public class CatchToken {
    public CatchToken(ObjectReference thrown) {
        this.thrown = thrown;
    }

    private final ObjectReference thrown;
}
