package cc.quarkus.qcc.graph.type;

public class IfToken {

    public static final IfToken TRUE = new IfToken(true);
    public static final IfToken FALSE = new IfToken(false);

    private IfToken(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return this.value;
    }

    public static IfToken of(boolean result) {
        if ( result ) {
            return TRUE;
        }
        return FALSE;
    }

    private final boolean value;
}
