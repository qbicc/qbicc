package org.qbicc.plugin.lowering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.LocalVariable;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.BooleanType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.LocalVariableElement;

/**
 *
 */
public final class LocalVariableLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, ValueHandle> {
    private final CompilationContext ctxt;
    private final Collection<LocalVariableElement> usedVariables;
    private final Map<LocalVariableElement, Value> allocatedVariables = new HashMap<>();
    private boolean started;

    public LocalVariableLoweringBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        this.usedVariables = Lowering.get(ctxt).removeUsedVariableSet(getCurrentElement());
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        final Node node = super.begin(blockLabel);
        if (! started) {
            started = true;
            final UnsignedIntegerType unsignedInteger8Type = ctxt.getTypeSystem().getUnsignedInteger8Type();
            final LiteralFactory lf = ctxt.getLiteralFactory();
            final IntegerLiteral one = lf.literalOf(1);
            // todo: allocate local variables closer to where they are used
            for (LocalVariableElement lve : usedVariables) {
                ValueType varType = lve.getType();
                if (varType instanceof BooleanType) {
                    // widen booleans to 8 bits
                    varType = unsignedInteger8Type;
                }
                int oldLine = setLineNumber(lve.getLine());
                int oldBci = setBytecodeIndex(lve.getBci());
                try {
                    Value address = stackAllocate(varType, one, lf.literalOf(varType.getAlign()));
                    allocatedVariables.put(lve, address);
                    declareDebugAddress(lve, address);
                } finally {
                    setLineNumber(oldLine);
                    setBytecodeIndex(oldBci);
                }
            }
        }
        return node;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        return super.load(transform(handle), mode);
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return super.store(transform(handle), value, mode);
    }

    @Override
    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndAdd(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseAnd(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseNand(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseOr(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseXor(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSet(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSetMax(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSetMin(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSub(transform(target), update, atomicityMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(transform(target), expect, update, successMode, failureMode, strength);
    }

    @Override
    public Value addressOf(ValueHandle handle) {
        return super.addressOf(transform(handle));
    }

    private ValueHandle transform(ValueHandle input) {
        return input.accept(this, null);
    }

    @Override
    public ValueHandle visitUnknown(Void param, ValueHandle node) {
        return node;
    }

    @Override
    public ValueHandle visit(Void param, ElementOf node) {
        ValueHandle orig = node.getValueHandle();
        ValueHandle transformed = transform(orig);
        return orig == transformed ? node : elementOf(transformed, node.getIndex());
    }

    @Override
    public ValueHandle visit(Void param, InstanceFieldOf node) {
        ValueHandle orig = node.getValueHandle();
        ValueHandle transformed = transform(orig);
        return orig == transformed ? node : instanceFieldOf(transformed, node.getVariableElement());
    }

    @Override
    public ValueHandle visit(Void param, MemberOf node) {
        ValueHandle orig = node.getValueHandle();
        ValueHandle transformed = transform(orig);
        return orig == transformed ? node : memberOf(transformed, node.getMember());
    }

    @Override
    public ValueHandle visit(Void param, LocalVariable node) {
        final Value pointer = allocatedVariables.get(node.getVariableElement());
        if (pointer == null) {
            throw new NoSuchElementException();
        }
        return pointerHandle(pointer);
    }
}
