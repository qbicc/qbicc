package cc.quarkus.qcc.plugin.opt;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public SimpleOptBasicBlockBuilder(final CompilationContext context, final BasicBlockBuilder delegate) {
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
