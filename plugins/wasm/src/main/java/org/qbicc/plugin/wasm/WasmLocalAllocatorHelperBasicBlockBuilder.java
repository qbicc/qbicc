package org.qbicc.plugin.wasm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.Literal;

/**
 * Local variable allocator support.
 * Insert splits before terminating blocks.
 */
public final class WasmLocalAllocatorHelperBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    public WasmLocalAllocatorHelperBasicBlockBuilder(FactoryContext fc, BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(targetPtr, receiver, arguments, catchLabel, splitArguments(targetArguments));
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, splitArguments(targetArguments));
    }

    @Override
    public BasicBlock goto_(BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.goto_(resumeLabel, splitArguments(targetArguments));
    }

    @Override
    public BasicBlock if_(Value condition, BlockLabel trueTarget, BlockLabel falseTarget, Map<Slot, Value> targetArguments) {
        return super.if_(condition, trueTarget, falseTarget, splitArguments(targetArguments));
    }

    @Override
    public BasicBlock switch_(Value value, int[] checkValues, BlockLabel[] targets, BlockLabel defaultTarget, Map<Slot, Value> targetArguments) {
        return super.switch_(value, checkValues, targets, defaultTarget, splitArguments(targetArguments));
    }

    @Override
    public BasicBlock ret(Value address, Map<Slot, Value> targetArguments) {
        return super.ret(address, splitArguments(targetArguments));
    }

    private Map<Slot, Value> splitArguments(final Map<Slot, Value> targetArguments) {
        return targetArguments.entrySet().stream().map(e -> {
            Value val = e.getValue();
            if (val instanceof Literal || val instanceof BlockParameter bp && bp.getSlot() == e.getKey()) {
                // no split needed
                return e;
            } else {
                return Map.entry(e.getKey(), split(e.getValue()));
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
