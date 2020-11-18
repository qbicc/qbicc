package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.literal.BooleanLiteral;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public SimpleOptBasicBlockBuilder(final BasicBlockBuilder delegate) {
        super(delegate);
    }

    public Value arrayLength(final Value array) {
        if (array instanceof NewArray) {
            return ((NewArray) array).getSize();
        } else {
            return getDelegate().arrayLength(array);
        }
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        if (condition instanceof BooleanLiteral) {
            return ((BooleanLiteral) condition).booleanValue() ? trueValue : falseValue;
        } else if (trueValue.equals(falseValue)) {
            return trueValue;
        } else {
            return getDelegate().select(condition, trueValue, falseValue);
        }
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        if (condition instanceof BooleanLiteral) {
            if (((BooleanLiteral) condition).booleanValue()) {
                return goto_(trueTarget);
            } else {
                return goto_(falseTarget);
            }
        } else {
            return getDelegate().if_(condition, trueTarget, falseTarget);
        }
    }
}
