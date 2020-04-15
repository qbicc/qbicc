package cc.quarkus.qcc.graph.type;

public class ShortValue implements Value<ShortType> {

    public ShortValue(short val) {
        this.val = val;
    }

    @Override
    public ShortType getType() {
        return ShortType.INSTANCE;
    }

    private final short val;
}
