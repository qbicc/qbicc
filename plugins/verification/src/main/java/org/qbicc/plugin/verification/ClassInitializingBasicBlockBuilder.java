package org.qbicc.plugin.verification;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
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
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 *
 */
public class ClassInitializingBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final CompilationContext ctxt;

    public ClassInitializingBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value instanceFieldOf(Value instance, InstanceFieldElement field) {
        initialize(field.getEnclosingType());
        return super.instanceFieldOf(instance, field);
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeStaticMember(sfl.getVariableElement().getEnclosingType());
        }
        return super.load(pointer, accessMode);
    }

    @Override
    public Node store(Value pointer, Value value, WriteAccessMode accessMode) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeStaticMember(sfl.getVariableElement().getEnclosingType());
        }
        return super.store(pointer, value, accessMode);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeStaticMember(sfl.getVariableElement().getEnclosingType());
        }
        return super.readModifyWrite(pointer, op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            initializeStaticMember(sfl.getVariableElement().getEnclosingType());
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
        initialize(type.getDefinition());
        return super.new_(type, typeId, size, align);
    }

    private Value initialize(final Value targetPtr) {
        if (targetPtr instanceof ExecutableLiteral el) {
            initialize(el.getExecutable().getEnclosingType());
        }
        return targetPtr;
    }

    private void initializeStaticMember(DefinedTypeDefinition definition) {
        if (definition.isInterface()) {
            initializeSingle(definition.load());
        } else {
            initialize(definition);
        }
    }

    private void initialize(DefinedTypeDefinition definition) {
        LoadedTypeDefinition ltd = definition.load();
        if (ltd.hasSuperClass()) {
            LoadedTypeDefinition superClass = ltd.getSuperClass();
            if (superClass != null) {
                initialize(superClass);
            }
        }
        maybeInitializeInterfaces(ltd);
        initializeSingle(ltd);
    }

    private void initializeSingle(final LoadedTypeDefinition ltd) {
        InitializerElement initializer = ltd.getInitializer();
        if (initializer != null) {
            ctxt.enqueue(initializer);
            initializer.tryCreateMethodBody();
        }
    }

    private void maybeInitializeInterfaces(LoadedTypeDefinition ltd) {
        int cnt = ltd.getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            LoadedTypeDefinition interfaceLtd = ltd.getInterface(i);
            if (interfaceLtd.declaresDefaultMethods()) {
                initialize(interfaceLtd);
            } else {
                maybeInitializeInterfaces(interfaceLtd);
            }
        }
    }
}
