package cc.quarkus.qcc.plugin.opt;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.type.NullType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public SimpleOptBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value cmpEq(final Value v1, final Value v2) {
        if (isAlwaysNull(v1) && isAlwaysNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        } else if (isNeverNull(v1) && isAlwaysNull(v2) || isAlwaysNull(v1) && isNeverNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        } else {
            return super.cmpEq(v1, v2);
        }
    }

    public Value cmpNe(final Value v1, final Value v2) {
        if (isAlwaysNull(v1) && isAlwaysNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        } else if (isNeverNull(v1) && isAlwaysNull(v2) || isAlwaysNull(v1) && isNeverNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        } else {
            return super.cmpNe(v1, v2);
        }
    }

    private boolean isAlwaysNull(final Value v1) {
        return v1.getType() instanceof NullType;
    }

    private boolean isNeverNull(final Value v1) {
        ValueType type = v1.getType();
        if (isAlwaysNull(v1)) {
            return false;
        }
        return ! (type instanceof ReferenceType) || ! ((ReferenceType) type).isNullable();
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
