package org.qbicc.plugin.verification;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

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
    public Value readModifyWrite(ValueHandle handle, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        if (handle instanceof PointerHandle ph && ph.getPointerValue() instanceof StaticFieldLiteral sfl) {
            initializeStaticMember(sfl.getVariableElement().getEnclosingType());
        }
        return super.readModifyWrite(handle, op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle handle, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        if (handle instanceof PointerHandle ph && ph.getPointerValue() instanceof StaticFieldLiteral sfl) {
            initializeStaticMember(sfl.getVariableElement().getEnclosingType());
        }
        return super.cmpAndSwap(handle, expect, update, readMode, writeMode, strength);
    }

    @Override
    public ValueHandle exactMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        initialize(method.getEnclosingType());
        return super.exactMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        initialize(method.getEnclosingType());
        return super.virtualMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle staticMethod(MethodElement method, MethodDescriptor callSiteDescriptor, StaticMethodType callSiteType) {
        initializeStaticMember(method.getEnclosingType());
        return super.staticMethod(method, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        initialize(constructor.getEnclosingType());
        return super.constructorOf(instance, constructor, callSiteDescriptor, callSiteType);
    }

    @Override
    public Value new_(ClassObjectType type, Value typeId, Value size, Value align) {
        initialize(type.getDefinition());
        return super.new_(type, typeId, size, align);
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
