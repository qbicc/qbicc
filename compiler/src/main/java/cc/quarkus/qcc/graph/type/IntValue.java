package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.CompareOp;

public class IntValue implements NumericValue<IntType, IntValue> {

    public IntValue(int val) {
        this.val = val;
    }

    @Override
    public IntType getType() {
        return IntType.INSTANCE;
    }

    @Override
    public String toString() {
        return "IntValue{" +
                "val=" + val +
                '}';
    }

    @Override
    public int intValue() {
        return this.val;
    }

    @Override
    public long longValue() {
        return this.val;
    }

    @Override
    public double doubleValue() {
        return this.val;
    }

    @Override
    public IntValue add(NumericValue<IntType,IntValue> other) {
        return new IntValue(intValue() + other.intValue());
    }

    @Override
    public IntValue sub(NumericValue<IntType,IntValue> other) {
        return new IntValue(intValue() - other.intValue());
    }

    @Override
    public IntValue div(NumericValue<IntType,IntValue> other) {
        return new IntValue(intValue() / other.intValue());
    }

    @Override
    public IntValue mul(NumericValue<IntType,IntValue> other) {
        return new IntValue(intValue() * other.intValue());
    }

    @Override
    public int compareTo(IntValue o) {
        return Integer.compare(intValue(), o.intValue());
    }

    private final int val;
}
