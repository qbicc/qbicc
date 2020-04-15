package cc.quarkus.qcc.graph.type;

public class BooleanValue implements Value<BooleanType> {

    public static final BooleanValue TRUE = new BooleanValue(true);
    public static final BooleanValue FALSE = new BooleanValue(true);

    private BooleanValue(boolean val) {
        this.val = val;
    }

    @Override
    public BooleanType getType() {
        return BooleanType.INSTANCE;
    }

    private final boolean val;
}
