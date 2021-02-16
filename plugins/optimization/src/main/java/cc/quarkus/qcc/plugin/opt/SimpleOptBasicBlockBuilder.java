package cc.quarkus.qcc.plugin.opt;

import static cc.quarkus.qcc.type.NullType.isAlwaysNull;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.ReferenceHandle;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;

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
        } else if (v1 instanceof Literal && v2 instanceof Literal && v1.getType().equals(v2.getType())) {
            // todo: replace with constant detection
            return ctxt.getLiteralFactory().literalOf(v1.equals(v2));
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

    public Value cmpLt(Value v1, Value v2) {
        // todo: replace with constant detection
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1 instanceof IntegerLiteral && v2 instanceof IntegerLiteral) {
            ValueType type = v1.getType();
            if (type.equals(v2.getType())) {
                long l1 = ((IntegerLiteral) v1).longValue();
                long l2 = ((IntegerLiteral) v2).longValue();
                if (type instanceof SignedIntegerType) {
                    return lf.literalOf(l1 < l2);
                } else {
                    assert type instanceof UnsignedIntegerType;
                    return lf.literalOf(Long.compareUnsigned(l1, l2) < 0);
                }
            }
        }
        return super.cmpLt(v1, v2);
    }

    public Value cmpGt(Value v1, Value v2) {
        // todo: replace with constant detection
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1 instanceof IntegerLiteral && v2 instanceof IntegerLiteral) {
            ValueType type = v1.getType();
            if (type.equals(v2.getType())) {
                long l1 = ((IntegerLiteral) v1).longValue();
                long l2 = ((IntegerLiteral) v2).longValue();
                if (type instanceof SignedIntegerType) {
                    return lf.literalOf(l1 > l2);
                } else {
                    assert type instanceof UnsignedIntegerType;
                    return lf.literalOf(Long.compareUnsigned(l1, l2) > 0);
                }
            }
        }
        return super.cmpGt(v1, v2);
    }

    public Value cmpLe(Value v1, Value v2) {
        // todo: replace with constant detection
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1 instanceof IntegerLiteral && v2 instanceof IntegerLiteral) {
            ValueType type = v1.getType();
            if (type.equals(v2.getType())) {
                long l1 = ((IntegerLiteral) v1).longValue();
                long l2 = ((IntegerLiteral) v2).longValue();
                if (type instanceof SignedIntegerType) {
                    return lf.literalOf(l1 <= l2);
                } else {
                    assert type instanceof UnsignedIntegerType;
                    return lf.literalOf(Long.compareUnsigned(l1, l2) <= 0);
                }
            }
        }
        return super.cmpLe(v1, v2);
    }

    public Value cmpGe(Value v1, Value v2) {
        // todo: replace with constant detection
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1 instanceof IntegerLiteral && v2 instanceof IntegerLiteral) {
            ValueType type = v1.getType();
            if (type.equals(v2.getType())) {
                long l1 = ((IntegerLiteral) v1).longValue();
                long l2 = ((IntegerLiteral) v2).longValue();
                if (type instanceof SignedIntegerType) {
                    return lf.literalOf(l1 >= l2);
                } else {
                    assert type instanceof UnsignedIntegerType;
                    return lf.literalOf(Long.compareUnsigned(l1, l2) >= 0);
                }
            }
        }
        return super.cmpGe(v1, v2);
    }

    public Value bitCast(Value input, WordType toType) {
        if (input instanceof BitCast) {
            final BitCast inputNode = (BitCast) input;
            if (inputNode.getInput().getType().equals(toType)) {
                // BitCast(BitCast(a, x), type-of a) -> a
                return inputNode.getInput();
            }

            // BitCast(BitCast(a, x), y) -> BitCast(a, y)
            return bitCast(inputNode.getInput(), toType);
        }
        return super.bitCast(input, toType);
    }

    private boolean isNeverNull(final Value v1) {
        ValueType type = v1.getType();
        if (isAlwaysNull(v1)) {
            return false;
        }
        return ! (type instanceof ReferenceType) || ! ((ReferenceType) type).isNullable();
    }

    public Value arrayLength(final ValueHandle arrayHandle) {
        if (arrayHandle instanceof ReferenceHandle) {
            Value array = ((ReferenceHandle) arrayHandle).getReferenceValue();
            if (array instanceof NewArray) {
                return ((NewArray) array).getSize();
            }
        }
        return getDelegate().arrayLength(arrayHandle);
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
