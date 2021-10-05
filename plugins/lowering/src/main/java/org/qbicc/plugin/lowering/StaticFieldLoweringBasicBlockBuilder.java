package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.object.Section;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;

/**
 *
 */
public class StaticFieldLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, ValueHandle> {
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition ourHolder;

    public StaticFieldLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        ourHolder = getCurrentElement().getEnclosingType();
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
    public ValueHandle visit(Void param, StaticField node) {
        final FieldElement fieldElement = node.getVariableElement();
        GlobalVariableElement global = Lowering.get(ctxt).getGlobalForField(fieldElement);
        DefinedTypeDefinition fieldHolder = fieldElement.getEnclosingType();
        if (! fieldHolder.equals(ourHolder)) {
            // we have to declare it in our translation unit
            Section section = ctxt.getOrAddProgramModule(ourHolder).getOrAddSection(global.getSection());
            section.declareData(fieldElement, global.getName(), global.getType());
        }
        return globalVariable(global);
    }

    @Override
    public ValueHandle visitUnknown(Void param, ValueHandle node) {
        // all other value handle kinds are returned as-is
        return node;
    }
}
