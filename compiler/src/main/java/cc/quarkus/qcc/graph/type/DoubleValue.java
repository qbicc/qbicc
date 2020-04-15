package cc.quarkus.qcc.graph.type;

public class DoubleValue implements Value<DoubleType> {

    public DoubleValue(double val) {
        this.val = val;
    }

    @Override
    public DoubleType getType() {
        return DoubleType.INSTANCE;
    }

    private final double val;
}
