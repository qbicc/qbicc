package org.qbicc.plugin.opt;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.ByteOffsetPointer;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.Node;
import org.qbicc.graph.OffsetPointer;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.CompoundType;

/**
 *
 */
public class LocalMemoryTrackingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private Map<Node, Value> knownValues = new HashMap<>();

    public LocalMemoryTrackingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        // other incoming edges might have loads which invalidate our local cache
        knownValues.clear();
        return super.begin(blockLabel);
    }

    @Override
    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        final Map<Node, Value> oldKnownValues = knownValues;
        knownValues = new HashMap<>();
        try {
            return super.begin(blockLabel, arg, maker);
        } finally {
            knownValues = oldKnownValues;
        }
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        // todo: hopefully we can be slightly more aggressive than this
        if (! GlobalPlain.includes(accessMode)) {
            knownValues.clear();
        } else {
            Value value = knownValues.get(pointer);
            if (value != null) {
                return value;
            }
        }
        // todo: keep a map of pointer -> "thing" for better alias detection
        Value loaded = super.load(pointer, accessMode);
        knownValues.put(pointer, loaded);
        return loaded;
    }

    @Override
    public Node store(Value pointer, Value value, WriteAccessMode accessMode) {
        Value root = findRoot(pointer);
        // todo: not completely correct but this class is temporarily disabled in any case
        knownValues.keySet().removeIf(k -> k instanceof Value ptr && ! hasSameRoot(ptr, root));
        knownValues.put(pointer, value);
        return super.store(pointer, value, accessMode);
    }

    @Override
    public Value readModifyWrite(ValueHandle target, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        knownValues.clear();
        return super.readModifyWrite(target, op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        knownValues.clear();
        return super.cmpAndSwap(pointer, expect, update, readMode, writeMode, strength);
    }

    @Override
    public Value memberOf(Value structPointer, CompoundType.Member member) {
        Value value = knownValues.get(structPointer);
        if (value != null) {
            return extractMember(value, member);
        } else {
            return super.memberOf(structPointer, member);
        }
    }

    @Override
    public Value elementOf(Value arrayPointer, Value index) {
        Value value = knownValues.get(arrayPointer);
        if (value != null) {
            return extractElement(value, index);
        } else {
            return super.elementOf(arrayPointer, index);
        }
    }

    @Override
    public Node fence(GlobalAccessMode fenceType) {
        knownValues.clear();
        return super.fence(fenceType);
    }

    @Override
    public Node monitorEnter(Value obj) {
        knownValues.clear();
        return super.monitorEnter(obj);
    }

    @Override
    public Node monitorExit(Value obj) {
        knownValues.clear();
        return super.monitorExit(obj);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        knownValues.clear();
        return super.call(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        knownValues.clear();
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        knownValues.clear();
        return super.invokeNoReturn(target, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        knownValues.clear();
        return super.tailCall(target, arguments);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        knownValues.clear();
        return super.invoke(target, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private static Value findRoot(Value pointer) {
        if (pointer instanceof MemberOf mo) {
            return findRoot(mo.getStructurePointer());
        } else if (pointer instanceof ElementOf eo) {
            return findRoot(eo.getArrayPointer());
        } else if (pointer instanceof InstanceFieldOf ifo) {
            return findRoot(ifo.getInstance());
        } else if (pointer instanceof DecodeReference dr) {
            return findRoot(dr.getInput());
        } else if (pointer instanceof OffsetPointer op) {
            return findRoot(op.getBasePointer());
        } else if (pointer instanceof ByteOffsetPointer bop) {
            return findRoot(bop.getBasePointer());
        } else {
            return pointer;
        }
    }

    private static boolean hasSameRoot(Value pointer, Value root) {
        return findRoot(pointer).equals(root);
    }
}
