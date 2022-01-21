package org.qbicc.plugin.opt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.Comp;
import org.qbicc.graph.Convert;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Extend;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.Neg;
import org.qbicc.graph.Node;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.WordCastValue;
import org.qbicc.graph.atomic.WriteAccessMode;
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
import org.qbicc.type.NullableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ConstructorElement;

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
        if (value instanceof Truncate trunc) {
            return truncate(trunc.getInput(), toType);
        }
        return super.truncate(value, toType);
    }

    @Override
    public Value extend(Value value, WordType toType) {
        Value result = literalCast(value, toType, false);
        return result != null ? result : super.extend(value, toType);
    }

    @Override
    public Value complement(Value v) {
        if (v instanceof Comp c) {
            return c.getInput();
        }
        final LiteralFactory lf = ctxt.getLiteralFactory();
        if (v instanceof IntegerLiteral il) {
            return lf.literalOf(il.getType(), ~il.longValue());
        }
        if (v.isDefEq(lf.literalOf(true))) {
            return lf.literalOf(false);
        } else if (v.isDefEq(lf.literalOf(false))) {
            return lf.literalOf(true);
        }
        return getDelegate().complement(v);
    }

    public Value isEq(final Value v1, final Value v2) {
        if (v1.isDefEq(v2)) {
            return ctxt.getLiteralFactory().literalOf(true);
        } else if (!v1.isNullable() && isAlwaysNull(v2) || isAlwaysNull(v1) && !v2.isNullable() || v1.isDefNe(v2)) {
            return ctxt.getLiteralFactory().literalOf(false);
        }

        if (v2.isDefEq(ctxt.getLiteralFactory().literalOf(false))) {
            return complement(v1);
        } else if (v1.isDefEq(ctxt.getLiteralFactory().literalOf(false))) {
            return complement(v2);
        }

        if ((v1 instanceof Extend || v1 instanceof BitCast && v1.getType() instanceof NullableType) && isZero(v2)) {
            Value input = ((WordCastValue) v1).getInput();
            // icmp eq iX (*ext/bitcast iY foo to iX), iX 0
            //   ↓
            // icmp eq iY foo, iY 0
            return isEq(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()));
        }
        if ((v2 instanceof Extend || v2 instanceof BitCast && v2.getType() instanceof NullableType) && isZero(v1)) {
            Value input = ((WordCastValue) v2).getInput();
            // icmp eq iX 0, iX (*ext/bitcast iY foo to iX)
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

        if (v2.isDefEq(ctxt.getLiteralFactory().literalOf(true))) {
            return complement(v1);
        } else if (v1.isDefEq(ctxt.getLiteralFactory().literalOf(true))) {
            return complement(v2);
        }

        if ((v1 instanceof Extend || v1 instanceof BitCast && v1.getType() instanceof NullableType) && isZero(v2)) {
            Value input = ((WordCastValue) v1).getInput();
            if (input.getType() instanceof BooleanType) {
                // icmp ne iX (zext i1 foo to iX), iX 0
                return input;
            } else {
                // icmp ne iX (*ext/bitcast iY foo to iX), iX 0
                return isNe(input, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()));
            }
        }
        if ((v2 instanceof Extend || v2 instanceof BitCast && v2.getType() instanceof NullableType) && isZero(v1)) {
            Value input = ((WordCastValue) v2).getInput();
            if (input.getType() instanceof BooleanType) {
                // icmp ne iX 0, iX (zext i1 foo to iX)
                return input;
            } else {
                // icmp ne iX 0, iX (*ext/bitcast iY foo to iX)
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
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1.isDefLt(v2)) {
            return lf.literalOf(true);
        } else if (v1.isDefGe(v2)) {
            return lf.literalOf(false);
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
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1.isDefGt(v2)) {
            return lf.literalOf(true);
        } else if (v1.isDefLe(v2)) {
            return lf.literalOf(false);
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
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1.isDefLe(v2)) {
            return lf.literalOf(true);
        } else if (v1.isDefGt(v2)) {
            return lf.literalOf(false);
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
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1.isDefGe(v2)) {
            return lf.literalOf(true);
        } else if (v1.isDefLt(v2)) {
            return lf.literalOf(false);
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

    @Override
    public Value and(Value v1, Value v2) {
        final LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1.getType() instanceof BooleanType) {
            // boolean reductions
            final BooleanLiteral trueLit = lf.literalOf(true);
            final BooleanLiteral falseLit = lf.literalOf(false);
            if (v1.isDefEq(trueLit) || v1.isDefNe(falseLit)) {
                return v2;
            } else if (v2.isDefEq(trueLit) || v2.isDefNe(falseLit)) {
                return v1;
            } else if (v1.isDefEq(falseLit) || v1.isDefNe(trueLit) || v2.isDefEq(falseLit) || v2.isDefNe(trueLit)) {
                return falseLit;
            } else if (v1 instanceof Comp c1 && v2 instanceof Comp c2) {
                // DeMorgan's theorem
                return complement(or(c1.getInput(), c2.getInput()));
            }
        } else if (v1.getType() instanceof IntegerType it) {
            // integer reductions
            if (v1 instanceof IntegerLiteral l1 && v2 instanceof IntegerLiteral l2) {
                return lf.literalOf(it, l1.longValue() & l2.longValue());
            }
            final IntegerLiteral zero = lf.literalOf(it, 0);
            if (v1.isDefEq(zero) || v2.isDefEq(zero)) {
                return zero;
            }
            final IntegerLiteral allOnes = lf.literalOf(it, -1L);
            if (v1.isDefEq(allOnes)) {
                return v2;
            } else if (v2.isDefEq(allOnes)) {
                return v1;
            }
        }
        return getDelegate().and(v1, v2);
    }

    @Override
    public Value or(Value v1, Value v2) {
        final LiteralFactory lf = ctxt.getLiteralFactory();
        if (v1.getType() instanceof BooleanType) {
            // boolean reductions
            final BooleanLiteral trueLit = lf.literalOf(true);
            final BooleanLiteral falseLit = lf.literalOf(false);
            if (v1.isDefNe(trueLit) || v1.isDefEq(falseLit)) {
                return v2;
            } else if (v2.isDefNe(trueLit) || v2.isDefEq(falseLit)) {
                return v1;
            } else if (v1.isDefNe(falseLit) || v1.isDefEq(trueLit) || v2.isDefNe(falseLit) || v2.isDefEq(trueLit)) {
                return trueLit;
            } else if (v1 instanceof Comp c1 && v2 instanceof Comp c2) {
                // DeMorgan's theorem
                return complement(and(c1.getInput(), c2.getInput()));
            }
        } else if (v1.getType() instanceof IntegerType it) {
            if (v1 instanceof IntegerLiteral l1 && v2 instanceof IntegerLiteral l2) {
                return lf.literalOf(it, l1.longValue() | l2.longValue());
            }
            // integer reductions
            final IntegerLiteral allOnes = lf.literalOf(it, -1L);
            if (v1.isDefEq(allOnes) || v2.isDefEq(allOnes)) {
                return allOnes;
            }
            final IntegerLiteral zero = lf.literalOf(it, 0);
            if (v1.isDefEq(zero)) {
                return v2;
            } else if (v2.isDefEq(zero)) {
                return v1;
            }
        }
        // use distributive law to reduce number of ops: AB ⋀ AC -> A ⋀ (B ⋁ C)
        if (v1 instanceof And a1 && v2 instanceof And a2) {
            final Value a1Left = a1.getLeftInput();
            final Value a2Left = a2.getLeftInput();
            final Value a1Right = a1.getRightInput();
            final Value a2Right = a2.getRightInput();
            if (a1Left.isDefEq(a2Left) || a2Left.isDefEq(a1Left)) {
                return and(a1Left, or(a1Right, a2Right));
            } else if (a1Left.isDefEq(a2Right) || a2Right.isDefEq(a1Left)) {
                return and(a1Left, or(a1Right, a2Left));
            } else if (a1Right.isDefEq(a2Left) || a2Left.isDefEq(a1Right)) {
                return and(a1Right, or(a1Left, a2Right));
            } else if (a1Right.isDefEq(a2Right) || a2Right.isDefEq(a1Right)) {
                return and(a1Right, or(a1Left, a2Left));
            }
        }
        return getDelegate().or(v1, v2);
    }

    @Override
    public Value xor(Value v1, Value v2) {
        if (v1.getType() instanceof BooleanType) {
            return isNe(v1, v2);
        }
        final LiteralFactory lf = ctxt.getLiteralFactory();
        IntegerType it = (IntegerType) v1.getType();
        if (v1 instanceof IntegerLiteral l1 && v2 instanceof IntegerLiteral l2) {
            return lf.literalOf(it, l1.longValue() ^ l2.longValue());
        }
        final IntegerLiteral allOnes = lf.literalOf(it, -1L);
        if (v1.isDefEq(allOnes)) {
            return complement(v2);
        } else if (v2.isDefEq(allOnes)) {
            return complement(v1);
        }
        final IntegerLiteral zero = lf.literalOf(it, 0);
        if (v1.isDefEq(zero)) {
            return v2;
        } else if (v2.isDefEq(zero)) {
            return v1;
        }
        return getDelegate().xor(v1, v2);
    }

    public Value bitCast(Value input, WordType toType) {
        if (input.getType().equals(toType)) {
            return input;
        }
        if (input.isDefEq(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(input.getType()))) {
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(toType);
        }
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
        if (condition instanceof Comp comp) {
            return select(comp.getInput(), falseValue, trueValue);
        }
        if (isEqualToLiteral(trueValue, 1) && isEqualToLiteral(falseValue, 0)) {
            return extend(condition, (WordType) trueValue.getType());
        } else if (isEqualToLiteral(trueValue, 0) && isEqualToLiteral(falseValue, 1)) {
            return extend(xor(condition, ctxt.getLiteralFactory().literalOf(true)), (WordType) trueValue.getType());
        }
        final BooleanLiteral trueLit = ctxt.getLiteralFactory().literalOf(true);
        final BooleanLiteral falseLit = ctxt.getLiteralFactory().literalOf(false);
        if (condition.isDefEq(trueLit) || condition.isDefNe(falseLit)) {
            return trueValue;
        } else if (condition.isDefEq(falseLit) || condition.isDefNe(trueLit)) {
            return falseValue;
        } else if (trueValue.equals(falseValue)) {
            return trueValue;
        } else if (trueValue.isDefEq(trueLit) && falseValue.isDefEq(falseLit)) {
            return condition;
        } else if (trueValue.isDefEq(falseLit) && falseValue.isDefEq(trueLit)) {
            return complement(condition);
        } else {
            return getDelegate().select(condition, trueValue, falseValue);
        }
    }

    @Override
    public Node store(ValueHandle handle, Value value, WriteAccessMode accessMode) {
        if (getCurrentElement() instanceof ConstructorElement
            && handle instanceof InstanceFieldOf ifo
            && ifo.getValueHandle() instanceof ReferenceHandle rh
            && rh.getReferenceValue() instanceof ParameterValue pv
            && pv.getLabel().equals("this")
            && value.isDefEq(ctxt.getLiteralFactory().zeroInitializerLiteralOfType(handle.getValueType()))
        ) {
            // skip it; the field is already zeroed
            return nop();
        }
        return super.store(handle, value, accessMode);
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        if (condition instanceof Comp comp) {
            return if_(comp.getInput(), falseTarget, trueTarget);
        }
        final BooleanLiteral trueLit = ctxt.getLiteralFactory().literalOf(true);
        final BooleanLiteral falseLit = ctxt.getLiteralFactory().literalOf(false);
        if (condition.isDefEq(trueLit) || condition.isDefNe(falseLit)) {
            return goto_(trueTarget);
        } else if (condition.isDefEq(falseLit) || condition.isDefNe(trueLit)) {
            return goto_(falseTarget);
        } else if (trueTarget == falseTarget) {
            return goto_(trueTarget);
        } else {
            return getDelegate().if_(condition, trueTarget, falseTarget);
        }
    }

    @Override
    public BasicBlock switch_(Value value, int[] checkValues, BlockLabel[] targets, BlockLabel defaultTarget) {
        if (value.getType() instanceof IntegerType it) {
            LiteralFactory lf = ctxt.getLiteralFactory();
            boolean defaultMatches = true;
            for (int checkValue : checkValues) {
                IntegerLiteral checkValueLit = lf.literalOf(it, checkValue);
                if (value.isDefEq(checkValueLit)) {
                    return goto_(targets[checkValue]);
                }
                if (defaultMatches && ! value.isDefNe(checkValueLit)) {
                    defaultMatches = false;
                }
            }
            if (defaultMatches) {
                return goto_(defaultTarget);
            }
        }
        return getDelegate().switch_(value, checkValues, targets, defaultTarget);
    }

    @Override
    public Value add(Value v1, Value v2) {
        if (v1.getType() instanceof IntegerType) {
            // integer opts
            assert v2.getType() instanceof IntegerType;
            if (isZero(v1)) {
                return v2;
            } else if (isZero(v2)) {
                return v1;
            } else if (v1 instanceof Neg n1) {
                return sub(v2, n1.getInput());
            } else if (v2 instanceof Neg n2) {
                return sub(v1, n2.getInput());
            }
        }
        return super.add(v1, v2);
    }

    @Override
    public Value sub(Value v1, Value v2) {
        if (v1.getType() instanceof IntegerType) {
            // integer opts
            assert v2.getType() instanceof IntegerType;
            if (isZero(v1)) {
                return negate(v2);
            } else if (isZero(v2)) {
                return v1;
            } else if (v2 instanceof Neg n2) {
                return add(v1, n2.getInput());
            }
        }
        return super.sub(v1, v2);
    }

    @Override
    public Value negate(Value v) {
        if (v instanceof Neg neg) {
            return neg.getInput();
        }
        if (isCmp(v)) {
            return cmp(cmpRight(v), cmpLeft(v));
        }

        return super.negate(v);
    }

    // handles

    @Override
    public Value addressOf(ValueHandle handle) {
        if (handle instanceof PointerHandle ph && isZero(ph.getOffsetValue())) {
            return ((PointerHandle) handle).getPointerValue();
        }
        return super.addressOf(handle);
    }

    @Override
    public ValueHandle pointerHandle(Value pointer, Value offsetValue) {
        if (pointer instanceof AddressOf) {
            if (isZero(offsetValue)) {
                return pointer.getValueHandle();
            } else if (pointer.getValueHandle() instanceof PointerHandle ph) {
                // merge the offset value
                return pointerHandle(ph.getPointerValue(), add(ph.getOffsetValue(), offsetValue));
            }
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
