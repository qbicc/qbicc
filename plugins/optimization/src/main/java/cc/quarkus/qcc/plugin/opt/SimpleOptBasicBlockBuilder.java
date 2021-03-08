package cc.quarkus.qcc.plugin.opt;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.AddressOf;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Cmp;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.NewArray;
import cc.quarkus.qcc.graph.PointerHandle;
import cc.quarkus.qcc.graph.ReferenceHandle;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;

import java.util.function.BiFunction;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public SimpleOptBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value isEq(final Value v1, final Value v2) {
        if (isAlwaysNull(v1) && isAlwaysNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        } else if (isNeverNull(v1) && isAlwaysNull(v2) || isAlwaysNull(v1) && isNeverNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        } else if (v1 instanceof Literal && v2 instanceof Literal && v1.getType().equals(v2.getType())) {
            // todo: replace with constant detection
            return ctxt.getLiteralFactory().literalOf(v1.equals(v2));
        }

        if (isCmp(v1) && isLiteral(v2, 0)) {
            return isEq(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, 0)) {
            return isEq(cmpLeft(v2), cmpRight(v2));
        }

        return super.isEq(v1, v2);
    }

    public Value isNe(final Value v1, final Value v2) {
        if (isAlwaysNull(v1) && isAlwaysNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        } else if (isNeverNull(v1) && isAlwaysNull(v2) || isAlwaysNull(v1) && isNeverNull(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        }

        if (isCmp(v1) && isLiteral(v2, 0)) {
            return isNe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, 0)) {
            return isNe(cmpLeft(v2), cmpRight(v2));
        }

        return super.isNe(v1, v2);
    }

    public Value isLt(Value v1, Value v2) {
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

        if (isCmp(v1) && isLiteral(v2, 1)) {
            return isLe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, -1)) {
            return isGe(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isLiteral(v2, 0)) {
            return isLt(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, 0)) {
            return isLt(cmpRight(v2), cmpLeft(v2));
        }

        return super.isLt(v1, v2);
    }

    public Value isGt(Value v1, Value v2) {
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

        if (isCmp(v2) && isLiteral(v1, 1)) {
            return isLe(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isLiteral(v2, -1)) {
            return isGe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v1) && isLiteral(v2, 0)) {
            return isGt(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, 0)) {
            return isGt(cmpRight(v2), cmpLeft(v2));
        }

        return super.isGt(v1, v2);
    }

    public Value isLe(Value v1, Value v2) {
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

        if (isCmp(v2) && isLiteral(v1, 1)) {
            return isGt(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isLiteral(v2, -1)) {
            return isLt(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v1) && isLiteral(v2, 0)) {
            return isLe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, 0)) {
            return isLe(cmpRight(v2), cmpLeft(v2));
        }

        return super.isLe(v1, v2);
    }

    public Value isGe(Value v1, Value v2) {
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

        if (isCmp(v1) && isLiteral(v2, -1)) {
            return isGe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, -1)) {
            return isLt(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isLiteral(v2, 0)) {
            return isGe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isLiteral(v1, 0)) {
            return isGe(cmpRight(v2), cmpLeft(v2));
        }

        return super.isGe(v1, v2);
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

    @Override
    public Value valueConvert(Value input, WordType toType) {
        if (input instanceof Convert) {
            Convert inputNode = (Convert) input;
            Value inputInput = inputNode.getInput();
            ValueType inputInputType = inputInput.getType();
            if (inputInputType.equals(toType)) {
                // Convert(Convert(a, x), type-of a) -> a
                return inputInput;
            }
            if (inputInputType instanceof PointerType && toType instanceof PointerType) {
                // Convert(Convert(a, x), y) -> BitCast(a, y) when a and y are pointer types
                return bitCast(inputInput, toType);
            }
            if (inputInputType instanceof ReferenceType && toType instanceof ReferenceType) {
                // Convert(Convert(a, x), y) -> BitCast(a, y) when a and y are reference types
                return bitCast(inputInput, toType);
            }
        }
        return super.valueConvert(input, toType);
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

    // special pointer behavior

    @Override
    public Value add(Value v1, Value v2) {
        // todo: maybe opt is not the right place for this
        if (v1.getType() instanceof PointerType) {
            return addressOf(elementOf(pointerHandle(v1), v2));
        } else if (v2.getType() instanceof PointerType) {
            return addressOf(elementOf(pointerHandle(v2), v1));
        }
        return super.add(v1, v2);
    }

    @Override
    public Value sub(Value v1, Value v2) {
        // todo: maybe opt is not the right place for this
        if (v1.getType() instanceof PointerType) {
            return addressOf(elementOf(pointerHandle(v1), negate(v2)));
        }
        return super.sub(v1, v2);
    }

    @Override
    public Value negate(Value v) {
        if (isCmp(v)) {
            return cmp(cmpRight(v), cmpLeft(v));
        }

        return super.negate(v);
    }

    // handles

    @Override
    public Value addressOf(ValueHandle handle) {
        if (handle instanceof PointerHandle) {
            return ((PointerHandle) handle).getPointerValue();
        }
        return super.addressOf(handle);
    }

    @Override
    public ValueHandle pointerHandle(Value pointer) {
        if (pointer instanceof AddressOf) {
            return pointer.getValueHandle();
        }
        return super.pointerHandle(pointer);
    }

    private static boolean isAlwaysNull(final Value value) {
        return value instanceof ZeroInitializerLiteral;
    }

    private static boolean isCmp(final Value value) {
        return value instanceof Cmp;
    }

    private static Value cmpLeft(final Value value) {
        return ((Cmp) value).getLeftInput();
    }

    private static Value cmpRight(final Value value) {
        return ((Cmp) value).getRightInput();
    }

    private boolean isLiteral(final Value value, final int literal) {
        return value instanceof IntegerLiteral &&
            ((IntegerLiteral) value).equals(ctxt.getLiteralFactory().literalOf(literal));
    }
}
