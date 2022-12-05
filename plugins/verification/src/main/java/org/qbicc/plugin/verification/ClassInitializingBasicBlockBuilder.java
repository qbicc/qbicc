package org.qbicc.plugin.verification;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.graph.literal.StaticMethodLiteral;
import org.qbicc.plugin.reachability.InitializerReachabilityFacts;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 *
 */
public class ClassInitializingBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final CompilationContext ctxt;
    private final Facts facts;

    public ClassInitializingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
        facts = Facts.get(this.ctxt);
    }

    @Override
    public Value instanceFieldOf(Value instance, InstanceFieldElement field) {
        initializeSingle(field.getEnclosingType().load());
        return super.instanceFieldOf(instance, field);
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeSingle(sfl.getVariableElement().getEnclosingType().load());
        }
        return super.load(pointer, accessMode);
    }

    @Override
    public Node store(Value pointer, Value value, WriteAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeSingle(sfl.getVariableElement().getEnclosingType().load());
        }
        return super.store(pointer, value, accessMode);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeSingle(sfl.getVariableElement().getEnclosingType().load());
        }
        return super.readModifyWrite(pointer, op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeSingle(sfl.getVariableElement().getEnclosingType().load());
        }
        return super.cmpAndSwap(pointer, expect, update, readMode, writeMode, strength);
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.call(initialize(targetPtr), receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoSideEffects(initialize(targetPtr), receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoReturn(initialize(targetPtr), receiver, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(initialize(targetPtr), receiver, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.tailCall(initialize(targetPtr), receiver, arguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(initialize(targetPtr), receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    @Override
    public Value new_(ClassObjectType type, Value typeId, Value size, Value align) {
        initializeSingle(type.getDefinition().load());
        return super.new_(type, typeId, size, align);
    }

    private Value initialize(final Value targetPtr) {
        if (targetPtr instanceof StaticMethodLiteral el) {
            initializeSingle(el.getExecutable().getEnclosingType().load());
        }
        return targetPtr;
    }

    private void initializeSingle(final LoadedTypeDefinition ltd) {
        InitializerElement initializer = ltd.getInitializer();
        if (initializer != null) {
            // ensure that constants are created
            initializer.tryCreateMethodBody();
            facts.discover(initializer, ExecutableReachabilityFacts.NEEDS_COMPILATION);
            facts.discover(initializer, InitializerReachabilityFacts.NEEDS_INITIALIZATION);
        }
    }
}
