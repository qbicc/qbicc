package org.qbicc.plugin.patcher;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public final class PatcherResolverBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final ClassContextPatchInfo info;

    private PatcherResolverBasicBlockBuilder(final ClassContextPatchInfo info, final BasicBlockBuilder delegate) {
        super(delegate);
        this.info = info;
    }

    public static BasicBlockBuilder createIfNeeded(FactoryContext ctxt, BasicBlockBuilder delegate) {
        ClassContext classContext = delegate.getCurrentClassContext();
        ClassContextPatchInfo info = Patcher.get(classContext.getCompilationContext()).get(classContext);
        if (info == null) {
            return delegate;
        }
        return new PatcherResolverBasicBlockBuilder(info, delegate);
    }

    @Override
    public Value new_(ClassTypeDescriptor desc) {
        return super.new_(info.transform(desc));
    }

    @Override
    public Value newArray(ArrayTypeDescriptor desc, Value size) {
        return super.newArray(info.transform(desc), size);
    }

    @Override
    public Value multiNewArray(ArrayTypeDescriptor desc, List<Value> dimensions) {
        return super.multiNewArray(info.transform(desc), dimensions);
    }

    @Override
    public Value checkcast(Value value, TypeDescriptor desc) {
        return super.checkcast(value, info.transform(desc));
    }

    @Override
    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.resolveStaticField(info.transform(owner), name, info.transform(type));
    }

    @Override
    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.staticMethod(info.transform(owner), name, info.transform(descriptor));
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.instanceFieldOf(instance, info.transform(owner), name, info.transform(type));
    }

    @Override
    public ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.exactMethodOf(instance, info.transform(owner), name, info.transform(descriptor));
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.virtualMethodOf(instance, info.transform(owner), name, info.transform(descriptor));
    }

    @Override
    public ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.interfaceMethodOf(instance, info.transform(owner), name, info.transform(descriptor));
    }

    @Override
    public ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        return super.constructorOf(instance, info.transform(owner), info.transform(descriptor));
    }

    @Override
    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.extractInstanceField(valueObj, info.transform(owner), name, info.transform(type));
    }

}
