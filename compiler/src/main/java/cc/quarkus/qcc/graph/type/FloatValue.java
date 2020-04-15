package cc.quarkus.qcc.graph.type;

public class FloatValue implements Value<FloatType> {

    public FloatValue(float val) {
        this.val = val;
    }

    @Override
    public FloatType getType() {
        return FloatType.INSTANCE;
    }

    private final float val;
}
