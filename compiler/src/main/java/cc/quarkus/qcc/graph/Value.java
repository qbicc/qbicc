package cc.quarkus.qcc.graph;

public interface Value extends Node {
    Value[] NO_VALUES = new Value[0];

    Value ICONST_0 = iconst(0);
    Value LCONST_0 = lconst(0);

    static Value iconst(int operand) {
        // todo: cache
        return new IntConstantValueImpl(operand);
    }

    static Value lconst(long operand) {
        // todo: cache
        return new LongConstantValueImpl(operand);
    }
}
