package org.qbicc.plugin.wasm;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.util.List;
import java.util.Map;

import org.eclipse.collections.api.factory.Maps;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.type.VoidType;

/**
 * A basic block builder which performs compatible lowering for WASM.
 */
public final class WasmCompatibleBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public WasmCompatibleBasicBlockBuilder(FactoryContext fc, BasicBlockBuilder delegate) {
        super(delegate);
    }

    // call transformation

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.call(targetPtr, receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoSideEffects(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        // just a regular call followed by a trap
        call(targetPtr, receiver, arguments);
        return unreachable();
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        // no tail call support
        return return_(call(targetPtr, receiver, arguments));
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        BlockLabel resumeLabel = new BlockLabel();
        begin(resumeLabel, BasicBlockBuilder::unreachable);
        invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
        return getTerminatedBlock();
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        if (true) {
            // TODO: no exception support yet
            Value res = call(targetPtr, receiver, arguments);
            if (! (res.getType() instanceof VoidType)) {
                targetArguments = Maps.immutable.ofMap(targetArguments).newWithKeyValue(Slot.result(), res).castToMap();
            }
            goto_(resumeLabel, targetArguments);
            return res;
        }

        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    // other terminators

    @Override
    public BasicBlock ret(Value address, Map<Slot, Value> targetArguments) {
        if (address instanceof BlockLiteral bl) {
            return goto_(bl.getBlockLabel(), targetArguments);
        } else if (address instanceof BlockParameter) {
            return super.ret(address, targetArguments);
        } else {
            getContext().error(getLocation(), "Ret instruction with unknowable return address value %s", address);
            return unreachable();
        }
    }

    // memory and atomics

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        return super.load(pointer, accessMode(accessMode));
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(pointer, op, update, accessMode(readMode), accessMode(writeMode));
    }

    @Override
    public Value cmpAndSwap(Value target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(target, expect, update, accessMode(readMode), accessMode(writeMode), CmpAndSwap.Strength.STRONG);
    }

    @Override
    public Node store(Value handle, Value value, WriteAccessMode accessMode) {
        return super.store(handle, value, accessMode(accessMode));
    }

    @Override
    public Node fence(GlobalAccessMode fenceType) {
        return super.fence(GlobalSeqCst);
    }

    @SuppressWarnings("unchecked")
    private static <A extends AccessMode> A accessMode(A input) {
        if (input == SinglePlain || input == GlobalPlain || input == SingleUnshared || input == GlobalUnshared) {
            return (A) SinglePlain;
        } else {
            return (A) SingleSeqCst;
        }
    }
}
