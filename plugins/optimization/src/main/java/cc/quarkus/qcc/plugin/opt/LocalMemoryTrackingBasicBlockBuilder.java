package org.qbicc.plugin.opt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public class LocalMemoryTrackingBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<MemoryAtomicityMode, Value> {
    private final CompilationContext ctxt;
    private final Map<ValueHandle, Value> knownValues = new HashMap<>();

    public LocalMemoryTrackingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        // other incoming edges might have loads which invalidate our local cache
        knownValues.clear();
        return super.begin(blockLabel);
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        // todo: hopefully we can be slightly more aggressive than this
        if (mode != MemoryAtomicityMode.NONE && mode != MemoryAtomicityMode.UNORDERED) {
            knownValues.clear();
        } else {
            Value value = handle.accept(this, mode);
            if (value != null) {
                return value;
            }
        }
        Value loaded = super.load(handle, mode);
        knownValues.put(handle, loaded);
        return loaded;
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        ValueHandle root = findRoot(handle);
        knownValues.keySet().removeIf(k -> ! hasSameRoot(k, root));
        knownValues.put(handle, value);
        return super.store(handle, value, mode);
    }

    @Override
    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndAdd(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndBitwiseAnd(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndBitwiseNand(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndBitwiseOr(target, update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndBitwiseXor(target, update, atomicityMode);
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndSet(target, update, atomicityMode);
    }

    @Override
    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndSetMax(target, update, atomicityMode);
    }

    @Override
    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndSetMin(target, update, atomicityMode);
    }

    @Override
    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        knownValues.clear();
        return super.getAndSub(target, update, atomicityMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode) {
        knownValues.clear();
        return super.cmpAndSwap(target, expect, update, successMode, failureMode);
    }

    @Override
    public Node fence(MemoryAtomicityMode fenceType) {
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
    public Node invokeStatic(MethodElement target, List<Value> arguments) {
        knownValues.clear();
        return super.invokeStatic(target, arguments);
    }

    @Override
    public Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        knownValues.clear();
        return super.invokeInstance(kind, instance, target, arguments);
    }

    @Override
    public Value invokeValueStatic(MethodElement target, List<Value> arguments) {
        knownValues.clear();
        return super.invokeValueStatic(target, arguments);
    }

    @Override
    public Value invokeConstructor(Value instance, ConstructorElement target, List<Value> arguments) {
        knownValues.clear();
        return super.invokeConstructor(instance, target, arguments);
    }

    @Override
    public Value invokeValueInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        knownValues.clear();
        return super.invokeValueInstance(kind, instance, target, arguments);
    }

    private static ValueHandle findRoot(ValueHandle handle) {
        return handle instanceof ElementOf || ! handle.hasValueHandleDependency() ? handle : findRoot(handle.getValueHandle());
    }

    private static boolean hasSameRoot(ValueHandle handle, ValueHandle root) {
        return findRoot(handle).equals(root);
    }

    @Override
    public Value visitUnknown(MemoryAtomicityMode param, ValueHandle node) {
        return knownValues.get(node);
    }

    @Override
    public Value visit(MemoryAtomicityMode param, ElementOf node) {
        Value value = knownValues.get(node);
        if (value != null) {
            return value;
        } else {
            value = node.getValueHandle().accept(this, param);
            if (value != null) {
                return extractElement(value, node.getIndex());
            } else {
                return null;
            }
        }
    }

    @Override
    public Value visit(MemoryAtomicityMode param, MemberOf node) {
        Value value = knownValues.get(node);
        if (value != null) {
            return value;
        } else {
            value = node.getValueHandle().accept(this, param);
            if (value != null) {
                return extractMember(value, node.getMember());
            } else {
                return null;
            }
        }
    }
}
