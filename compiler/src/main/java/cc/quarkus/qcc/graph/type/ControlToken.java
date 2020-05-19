package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.QType;

public final class ControlToken implements QType {

    public static final ControlToken NO_CONTROL = new ControlToken();
    public static final ControlToken CONTROL = new ControlToken();

    private ControlToken() {

    }

    @Override
    public String toString() {
        if ( this == NO_CONTROL ) {
            return "<no-control>";
        }
        return "<control>";
    }
}

