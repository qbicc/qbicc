package org.qbicc.plugin.opt;

import static org.qbicc.graph.atomic.AccessModes.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.PointerValueVisitor;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;

/**
 *
 */
public class LocalMemoryTrackingBasicBlockBuilder extends DelegatingBasicBlockBuilder implements PointerValueVisitor<AccessMode, Value> {
    private Map<PointerValue, Value> knownValues = new HashMap<>();

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
        final Map<PointerValue, Value> oldKnownValues = knownValues;
        knownValues = new HashMap<>();
        try {
            return super.begin(blockLabel, arg, maker);
        } finally {
            knownValues = oldKnownValues;
        }
    }

    @Override
    public Value load(PointerValue handle, ReadAccessMode accessMode) {
        // todo: hopefully we can be slightly more aggressive than this
        if (! GlobalPlain.includes(accessMode)) {
            knownValues.clear();
        } else {
            Value value = handle.accept(this, accessMode);
            if (value != null) {
                return value;
            }
        }
        Value loaded = super.load(handle, accessMode);
        knownValues.put(handle, loaded);
        return loaded;
    }

    @Override
    public Node store(PointerValue handle, Value value, WriteAccessMode accessMode) {
        PointerValue root = findRoot(handle);
        knownValues.keySet().removeIf(k -> ! hasSameRoot(k, root));
        knownValues.put(handle, value);
        return super.store(handle, value, accessMode);
    }

    @Override
    public Value readModifyWrite(PointerValue target, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        knownValues.clear();
        return super.readModifyWrite(target, op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(PointerValue target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        knownValues.clear();
        return super.cmpAndSwap(target, expect, update, readMode, writeMode, strength);
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
    public Value call(PointerValue target, List<Value> arguments) {
        knownValues.clear();
        return super.call(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(PointerValue target, List<Value> arguments) {
        knownValues.clear();
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(PointerValue target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        knownValues.clear();
        return super.invokeNoReturn(target, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(PointerValue target, List<Value> arguments) {
        knownValues.clear();
        return super.tailCall(target, arguments);
    }

    @Override
    public BasicBlock tailInvoke(PointerValue target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        knownValues.clear();
        return super.tailInvoke(target, arguments, catchLabel, targetArguments);
    }

    @Override
    public Value invoke(PointerValue target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        knownValues.clear();
        return super.invoke(target, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private static PointerValue findRoot(PointerValue handle) {
        return handle instanceof ElementOf || ! handle.hasPointerValueDependency() ? handle : findRoot(handle.getPointerValue());
    }

    private static boolean hasSameRoot(PointerValue handle, PointerValue root) {
        return findRoot(handle).equals(root);
    }

    @Override
    public Value visitUnknown(AccessMode param, PointerValue node) {
        return knownValues.get(node);
    }

    @Override
    public Value visit(AccessMode param, ElementOf node) {
        Value value = knownValues.get(node);
        if (value != null) {
            return value;
        } else {
            value = node.getPointerValue().accept(this, param);
            if (value != null) {
                return extractElement(value, node.getIndex());
            } else {
                return null;
            }
        }
    }

    @Override
    public Value visit(AccessMode param, MemberOf node) {
        Value value = knownValues.get(node);
        if (value != null) {
            return value;
        } else {
            value = node.getPointerValue().accept(this, param);
            if (value != null) {
                return extractMember(value, node.getMember());
            } else {
                return null;
            }
        }
    }
}
