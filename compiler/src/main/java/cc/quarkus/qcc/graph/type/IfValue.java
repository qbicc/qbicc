package cc.quarkus.qcc.graph.type;

public class IfValue implements Value<IfType, IfValue> {

    public static final IfValue TRUE = new IfValue(true);
    public static final IfValue FALSE = new IfValue(false);

    private IfValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return this.value;
    }

    public static IfValue of(boolean result) {
        if ( result ) {
            return TRUE;
        }
        return FALSE;
    }

    @Override
    public IfType getType() {
        return IfType.INSTANCE;
    }

    private final boolean value;
}
