package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;

/**
 * A basic block builder which transforms memory accesses to {@code boolean} fields which have been widened
 * to 8 bits.
 */
public final class BooleanAccessBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public BooleanAccessBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        return intToBool(handle, super.load(handle, mode));
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return super.store(handle, boolToInt(handle, value), mode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode) {
        Value result = super.cmpAndSwap(target, boolToInt(target, expect), boolToInt(target, update), successMode, failureMode);
        // the result is a { i8, i1 } if the field is boolean
        if (target.getValueType() instanceof BooleanType) {
            // we need to change to a { i1, i1 }
            TypeSystem ts = ctxt.getTypeSystem();
            LiteralFactory lf = ctxt.getLiteralFactory();
            UnsignedIntegerType u8 = ts.getUnsignedInteger8Type();
            BooleanType bool = ts.getBooleanType();
            CompoundType origType = CmpAndSwap.getResultType(ctxt, u8);
            CompoundType newType = CmpAndSwap.getResultType(ctxt, bool);
            Value resultByteVal = extractMember(result, origType.getMember(0));
            Value resultFlag = extractMember(result, origType.getMember(1));
            result = insertMember(lf.zeroInitializerLiteralOfType(newType), newType.getMember(0), truncate(resultByteVal, bool));
            result = insertMember(result, newType.getMember(1), resultFlag);
        }
        return result;
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseAnd(target, boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseNand(target, boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseOr(target, boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseXor(target, boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndSet(target, boolToInt(target, update), atomicityMode));
    }


    private Value boolToInt(final ValueHandle handle, final Value value) {
        if (handle.getValueType() instanceof BooleanType) {
            // we have to widen the value to an integer
            return extend(value, ctxt.getTypeSystem().getUnsignedInteger8Type());
        }
        return value;
    }

    private Value intToBool(final ValueHandle handle, final Value value) {
        ValueType valueType = handle.getValueType();
        if (valueType instanceof BooleanType) {
            // narrow it back
            return truncate(value, (BooleanType) valueType);
        }
        return value;
    }
}
