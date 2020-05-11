package cc.quarkus.qcc.graph.type;

public final class ControlToken {

    public static final ControlToken NO_CONTROL = new ControlToken();
    public static final ControlToken CONTROL = new ControlToken();

    private ControlToken() {

    }
}
