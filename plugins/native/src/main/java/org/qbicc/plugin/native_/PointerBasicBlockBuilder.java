package org.qbicc.plugin.native_;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ArrayType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.VoidType;

/**
 * A basic block builder which handles pointer type manipulations.
 */
public class PointerBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public PointerBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value decodeReference(Value reference, PointerType pointerType) {
        if (reference.getType() instanceof PointerType pt) {
            return pt.equals(pointerType) ? reference : getFirstBuilder().bitCast(reference, pointerType);
        } else if (reference.getType() instanceof ArrayType) {
            ctxt.error(getLocation(), "Cannot directly reference an array");
            throw new BlockEarlyTermination(unreachable());
        } else {
            return super.decodeReference(reference, pointerType);
        }
    }

    private Value castVoidPointer(Value value, ValueType toType) {
        if (toType instanceof PointerType outputPtr && value.getType() instanceof PointerType inputPtr) {
            if (inputPtr.getPointeeType() instanceof VoidType || outputPtr.getPointeeType() instanceof VoidType) {
                // they can be freely cast
                return getFirstBuilder().bitCast(value, outputPtr);
            } else if (! outputPtr.getPointeeType().isImplicitlyConvertibleFrom(inputPtr.getPointeeType())){
                ctxt.error(getLocation(), "Invalid pointer conversion from %s to %s", inputPtr, outputPtr);
            }
        }
        return value;
    }

    private List<Value> castVoidPointers(List<Value> arguments, Value target) {
        int sz = arguments.size();
        InvokableType callSiteType = target.getPointeeType(InvokableType.class);
        int parameterCount = callSiteType.getParameterCount();
        int actualCnt;
        if (parameterCount > 0 && callSiteType.getLastParameterType(0) instanceof VariadicType) {
            // do not map variadic arguments
            actualCnt = parameterCount - 1;
        } else {
            actualCnt = sz;
        }
        for (int i = 0; i < actualCnt; i ++) {
            Value arg = arguments.get(i);
            if (arg != castVoidPointer(arg, callSiteType.getParameterType(i))) {
                List<Value> newArgs = new ArrayList<>(sz);
                int j = 0;
                for (; j < actualCnt; j ++) {
                    newArgs.add(castVoidPointer(arguments.get(j), callSiteType.getParameterType(j)));
                }
                // variadic arguments (if any)
                for (; j < sz; j ++) {
                    newArgs.add(arguments.get(j));
                }
                return newArgs;
            }
        }
        return arguments;
    }

    @Override
    public Node store(Value pointer, Value value, WriteAccessMode accessMode) {
        return super.store(pointer, castVoidPointer(value, pointer.getPointeeType()), accessMode);
    }

    @Override
    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(pointer, castVoidPointer(expect, pointer.getPointeeType()), castVoidPointer(update, pointer.getPointeeType()), readMode, writeMode, strength);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(pointer, op, castVoidPointer(update, pointer.getPointeeType()), readMode, writeMode);
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.call(targetPtr, receiver, castVoidPointers(arguments, targetPtr));
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoSideEffects(targetPtr, receiver, castVoidPointers(arguments, targetPtr));
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoReturn(targetPtr, receiver, castVoidPointers(arguments, targetPtr));
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(targetPtr, receiver, castVoidPointers(arguments, targetPtr), catchLabel, targetArguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(targetPtr, receiver, castVoidPointers(arguments, targetPtr), catchLabel, resumeLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.tailCall(targetPtr, receiver, castVoidPointers(arguments, targetPtr));
    }
}
