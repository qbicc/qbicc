package cc.quarkus.qcc.graph.type;

public class IntValue implements Value<IntType> {

    public IntValue(int val) {
        this.val = val;
    }

    @Override
    public IntType getType() {
        return IntType.INSTANCE;
    }

    private final int val;
}
