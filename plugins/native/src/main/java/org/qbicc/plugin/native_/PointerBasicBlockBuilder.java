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
import org.qbicc.graph.Executable;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ArrayType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
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
    public ValueHandle referenceHandle(Value reference) {
        if (reference.getType() instanceof PointerType) {
            return pointerHandle(reference);
        } else if (reference.getType() instanceof ArrayType) {
            ctxt.error(getLocation(), "Cannot directly reference an array");
            throw new BlockEarlyTermination(unreachable());
        } else {
            return super.referenceHandle(reference);
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

    private List<Value> castVoidPointers(List<Value> arguments, ValueHandle target) {
        return target instanceof Executable ex ? castVoidPointers(arguments, ex) : arguments;
    }

    private List<Value> castVoidPointers(List<Value> arguments, Executable target) {
        int sz = arguments.size();
        InvokableType callSiteType = target.getCallSiteType();
        for (int i = 0; i < sz; i ++) {
            Value arg = arguments.get(i);
            if (arg != castVoidPointer(arg, callSiteType.getParameterType(i))) {
                List<Value> newArgs = new ArrayList<>(sz);
                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < sz; j ++) {
                    newArgs.add(castVoidPointer(arguments.get(j), callSiteType.getParameterType(i)));
                }
                return newArgs;
            }
        }
        return arguments;
    }

    @Override
    public Node store(ValueHandle handle, Value value, WriteAccessMode accessMode) {
        return super.store(handle, castVoidPointer(value, handle.getPointeeType()), accessMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(target, castVoidPointer(expect, target.getPointeeType()), castVoidPointer(update, target.getPointeeType()), readMode, writeMode, strength);
    }

    @Override
    public Value readModifyWrite(ValueHandle target, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(target, op, castVoidPointer(update, target.getPointeeType()), readMode, writeMode);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        return super.call(target, castVoidPointers(arguments, target));
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return super.callNoSideEffects(target, castVoidPointers(arguments, target));
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        return super.callNoReturn(target, castVoidPointers(arguments, target));
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(target, castVoidPointers(arguments, target), catchLabel, targetArguments);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(target, castVoidPointers(arguments, target), catchLabel, resumeLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return super.tailCall(target, castVoidPointers(arguments, target));
    }
}
