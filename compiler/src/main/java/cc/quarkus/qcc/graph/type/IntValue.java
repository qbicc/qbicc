package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.graph.node.CompareOp;

public class IntValue implements Value<IntType> {

    public IntValue(int val) {
        this.val = val;
    }

    @Override
    public IntType getType() {
        return IntType.INSTANCE;
    }

    @Override
    public boolean compare(CompareOp op, Value<IntType> other) {
        System.err.println( "COMPOP " + this + " " + op + " " + other);
        if ( other instanceof IntValue ) {
            switch ( op ) {
                case EQUAL:
                    return this.val == ((IntValue) other).val;
                case NOT_EQUAL:
                    return this.val != ((IntValue) other).val;
                case LESS_THAN:
                    return this.val < ((IntValue) other).val;
                case LESS_THAN_OR_EQUAL:
                    return this.val <= ((IntValue) other).val;
                case GREATER_THAN:
                    return this.val > ((IntValue) other).val;
                case GREATER_THAN_OR_EQUAL:
                    return this.val >= ((IntValue) other).val;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "IntValue{" +
                "val=" + val +
                '}';
    }

    private final int val;
}
