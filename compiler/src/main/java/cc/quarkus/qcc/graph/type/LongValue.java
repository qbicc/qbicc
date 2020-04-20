package cc.quarkus.qcc.graph.type;

public class LongValue implements Value<LongType, LongValue> {

    public LongValue(long val) {
        this.val = val;
    }

    @Override
    public LongType getType() {
        return LongType.INSTANCE;
    }

    private final long val;
}
