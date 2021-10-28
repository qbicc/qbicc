package org.qbicc.plugin.verification;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public class ClassInitializingBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final CompilationContext ctxt;

    public ClassInitializingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        initialize(field.getEnclosingType());
        return super.instanceFieldOf(instance, field);
    }

    @Override
    public ValueHandle staticField(FieldElement field) {
        initializeStaticMember(field.getEnclosingType());
        return super.staticField(field);
    }

    @Override
    public ValueHandle exactMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        initialize(method.getEnclosingType());
        return super.exactMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        initialize(method.getEnclosingType());
        return super.virtualMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle staticMethod(MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        initializeStaticMember(method.getEnclosingType());
        return super.staticMethod(method, callSiteDescriptor, callSiteType);
    }

    @Override
    public ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        initialize(constructor.getEnclosingType());
        return super.constructorOf(instance, constructor, callSiteDescriptor, callSiteType);
    }

    @Override
    public Value new_(ClassObjectType type) {
        initialize(type.getDefinition());
        return super.new_(type);
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
