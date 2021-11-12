package org.qbicc.plugin.opt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.Convert;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Extend;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;

/**
 * A graph factory which performs simple optimizations opportunistically.
 */
public class SimpleOptBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public SimpleOptBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value extractElement(Value array, Value index) {
        final Value value = array.extractElement(ctxt.getLiteralFactory(), index);
        if (value != null) {
            return value;
        } else {
            return super.extractElement(array, index);
        }
    }

    @Override
    public Value extractMember(Value compound, CompoundType.Member member) {
        final Value value = compound.extractMember(ctxt.getLiteralFactory(), member);
        if (value != null) {
            return value;
        } else {
            return super.extractMember(compound, member);
        }
    }

    @Override
    public Value insertElement(Value array, Value index, Value value) {
        if (array instanceof ArrayLiteral al && index instanceof IntegerLiteral il && value instanceof Literal lit) {
            final LiteralFactory lf = ctxt.getLiteralFactory();
            Literal[] values = al.getValues().toArray(Literal[]::new);
            values[il.intValue()] = lit;
            return lf.literalOf(al.getType(), List.of(values));
        }
        return super.insertElement(array, index, value);
    }

    @Override
    public Value insertMember(Value compound, CompoundType.Member member, Value value) {
        if (compound instanceof CompoundLiteral cl && value instanceof Literal lit) {
            final LiteralFactory lf = ctxt.getLiteralFactory();
            final Map<CompoundType.Member, Literal> values = cl.getValues();
            final HashMap<CompoundType.Member, Literal> copy = new HashMap<>(values);
            copy.put(member, lit);
            return lf.literalOf(cl.getType(), Map.copyOf(copy));
        }
        return super.insertMember(compound, member, value);
    }

    private Value literalCast(Value value, WordType toType, boolean truncate) {
        if (value instanceof IntegerLiteral) {
            if (toType instanceof IntegerType) {
                return ctxt.getLiteralFactory().literalOf((IntegerType) toType, ((IntegerLiteral) value).longValue());
            } else if (toType instanceof BooleanType) {
                long longValue = ((IntegerLiteral) value).longValue();
                return ctxt.getLiteralFactory().literalOf((truncate ? (longValue & 1) : longValue) != 0);
            } else if (toType instanceof FloatType) {
                return ctxt.getLiteralFactory().literalOf((FloatType) toType, ((IntegerLiteral) value).longValue());
            }
        } else if (value instanceof FloatLiteral) {
            if (toType instanceof IntegerType) {
                return ctxt.getLiteralFactory().literalOf((IntegerType) toType, (long) ((FloatLiteral) value).doubleValue());
            } else if (toType instanceof FloatType) {
                return ctxt.getLiteralFactory().literalOf((FloatType) toType, ((FloatLiteral) value).doubleValue());
            } else if (toType instanceof BooleanType) {
                assert truncate;
                return ctxt.getLiteralFactory().literalOf((((long) ((FloatLiteral) value).doubleValue()) & 1) != 0);
            }
        } else if (value instanceof BooleanLiteral) {
            if (toType instanceof IntegerType) {
                return ctxt.getLiteralFactory().literalOf((IntegerType) toType, ((BooleanLiteral) value).booleanValue() ? 1 : 0);
            }
        }
        return null;
    }

    @Override
    public Value truncate(Value value, WordType toType) {
        Value result = literalCast(value, toType, true);
        if (result != null) {
            return result;
        }
        if (value instanceof Truncate) {
            return truncate(((Truncate) value).getInput(), toType);
        }
        return super.truncate(value, toType);
    }

    @Override
    public Value extend(Value value, WordType toType) {
        Value result = literalCast(value, toType, false);
        return result != null ? result : super.extend(value, toType);
    }

    public Value isEq(final Value v1, final Value v2) {
        if (v1.isDefEq(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        } else if (!v1.isNullable() && isAlwaysNull(v2) || isAlwaysNull(v1) && !v2.isNullable() || v1.isDefNe(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        }

        if (v1 instanceof Extend && isZero(v2)) {
            Value input = ((Extend) v1).getInput();
            // icmp eq iX (*ext iY foo to iX), iX 0
            //   ↓
            // icmp eq iY foo, iY 0
            return isEq(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()));
        }
        if (v2 instanceof Extend && isZero(v1)) {
            Value input = ((Extend) v2).getInput();
            // icmp eq iX 0, iX (*ext iY foo to iX)
            //   ↓
            // icmp eq iY 0, iY foo
            return isEq(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()), input);
        }

        if (isCmp(v1) && isEqualToLiteral(v2, 0)) {
            return isEq(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, 0)) {
            return isEq(cmpLeft(v2), cmpRight(v2));
        }

        return super.isEq(v1, v2);
    }

    public Value isNe(final Value v1, final Value v2) {
        if (v1.isDefEq(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        } else if (!v1.isNullable() && isAlwaysNull(v2) || isAlwaysNull(v1) && !v2.isNullable() || v1.isDefNe(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        }

        if (v1 instanceof Extend && isZero(v2)) {
            Value input = ((Extend) v1).getInput();
            if (input.getType() instanceof BooleanType) {
                // icmp ne iX (zext i1 foo to iX), iX 0
                return input;
            } else {
                // icmp ne iX (*ext iY foo to iX), iX 0
                return isNe(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()));
            }
        }
        if (v2 instanceof Extend && isZero(v1)) {
            Value input = ((Extend) v2).getInput();
            if (input.getType() instanceof BooleanType) {
                // icmp ne iX 0, iX (zext i1 foo to iX)
                return input;
            } else {
                // icmp ne iX 0, iX (*ext iY foo to iX)
                return isNe(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()), input);
            }
        }

        if (isCmp(v1) && isEqualToLiteral(v2, 0)) {
            return isNe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, 0)) {
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

        if (isCmp(v1) && isEqualToLiteral(v2, 1)) {
            return isLe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, -1)) {
            return isGe(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isEqualToLiteral(v2, 0)) {
            return isLt(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, 0)) {
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

        if (isCmp(v2) && isEqualToLiteral(v1, 1)) {
            return isLe(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isEqualToLiteral(v2, -1)) {
            return isGe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v1) && isEqualToLiteral(v2, 0)) {
            return isGt(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, 0)) {
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

        if (isCmp(v2) && isEqualToLiteral(v1, 1)) {
            return isGt(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isEqualToLiteral(v2, -1)) {
            return isLt(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v1) && isEqualToLiteral(v2, 0)) {
            return isLe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, 0)) {
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

        if (isCmp(v1) && isEqualToLiteral(v2, -1)) {
            return isGe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, -1)) {
            return isLt(cmpLeft(v2), cmpRight(v2));
        }
        if (isCmp(v1) && isEqualToLiteral(v2, 0)) {
            return isGe(cmpLeft(v1), cmpRight(v1));
        }
        if (isCmp(v2) && isEqualToLiteral(v1, 0)) {
            return isGe(cmpRight(v2), cmpLeft(v2));
        }

        return super.isGe(v1, v2);
    }

    public Value bitCast(Value input, WordType toType) {
        if (input instanceof final BitCast inputNode) {
            if (inputNode.getInput().getType().equals(toType)) {
                // BitCast(BitCast(a, x), type-of a) -> a
                return inputNode.getInput();
            }

            // BitCast(BitCast(a, x), y) -> BitCast(a, y)
            return bitCast(inputNode.getInput(), toType);
        } else if (input.getType() instanceof PointerType inPtrType && toType instanceof PointerType outPtrType) {
            // pointer to struct/array -> pointer to first member/element
            if (inPtrType.getPointeeType() instanceof CompoundType) {
                final IntegerLiteral z = ctxt.getLiteralFactory().literalOf(0);
                ValueHandle outVal = addressOfFirst(pointerHandle(input, z), outPtrType.getPointeeType());
                if (outVal != null) {
                    return addressOf(outVal);
                }
            }
        }
        return super.bitCast(input, toType);
    }

    private ValueHandle addressOfFirst(final ValueHandle input, final ValueType outputType) {
        // if the output type matches the first member or element of input, return its handle
        if (input.getValueType() instanceof CompoundType ct && ct.getMemberCount() > 0) {
            final CompoundType.Member memberZero = ct.getMember(0);
            if (memberZero.getOffset() == 0) {
                ValueHandle nextHandle = memberOf(input, memberZero);
                if (outputType.equals(memberZero.getType())) {
                    return nextHandle;
                } else {
                    return addressOfFirst(nextHandle, outputType);
                }
            }
        } else if (input.getValueType() instanceof ArrayType at && at.getElementCount() > 0) {
            ValueHandle nextHandle = elementOf(input, ctxt.getLiteralFactory().literalOf(0));
            if (outputType.equals(at.getElementType())) {
                return nextHandle;
            } else {
                return addressOfFirst(nextHandle, outputType);
            }
        }
        return null;
    }

    @Override
    public Value valueConvert(Value input, WordType toType) {
        Value result = literalCast(input, toType, false);
        if (result != null) {
            return result;
        }
        if (input instanceof Convert inputNode) {
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
            return addressOf(pointerHandle(v1, v2));
        } else if (v2.getType() instanceof PointerType) {
            return addressOf(pointerHandle(v2, v1));
        }
        return super.add(v1, v2);
    }

    @Override
    public Value sub(Value v1, Value v2) {
        // todo: maybe opt is not the right place for this
        if (v1.getType() instanceof PointerType) {
            return addressOf(pointerHandle(v1, negate(v2)));
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
        if (handle instanceof PointerHandle ph) {
            final Value offsetValue = ph.getOffsetValue();
            if (offsetValue.getType() instanceof IntegerType it && offsetValue.isDefEq(ctxt.getLiteralFactory().literalOf(it, 0))) {
                return ((PointerHandle) handle).getPointerValue();
            }
        }
        return super.addressOf(handle);
    }

    @Override
    public ValueHandle pointerHandle(Value pointer, Value offsetValue) {
        if (pointer instanceof AddressOf) {
            return pointer.getValueHandle();
        }
        return super.pointerHandle(pointer, offsetValue);
    }

    private static boolean isAlwaysNull(final Value value) {
        return value instanceof NullLiteral;
    }

    private boolean isZero(final Value value) {
        return value.isDefEq(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(value.getType()));
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

    private boolean isEqualToLiteral(final Value value, final int literal) {
        return value.getType() instanceof IntegerType it &&
            value.isDefEq(ctxt.getLiteralFactory().literalOf(it, literal));
    }
}
