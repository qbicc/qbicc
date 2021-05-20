package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public class NativeBindingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final ExecutableElement rootElement;

    public NativeBindingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        rootElement = getCurrentElement();
    }

    @Override
    public ValueHandle staticMethod(MethodElement method) {
        MethodElement binding = NativeInfo.get(ctxt).getNativeBinding(method);
        return super.staticMethod(binding != null ? binding : method);
    }

    @Override
    public ValueHandle exactMethodOf(Value instance, MethodElement method) {
        MethodElement binding = NativeInfo.get(ctxt).getNativeBinding(method);
        return super.exactMethodOf(instance, binding != null ? binding : method);
    }

    @Override
    public ValueHandle virtualMethodOf(Value instance, MethodElement method) {
        MethodElement binding = NativeInfo.get(ctxt).getNativeBinding(method);
        return super.virtualMethodOf(instance, binding != null ? binding : method);
    }

    @Override
    public ValueHandle interfaceMethodOf(Value instance, MethodElement method) {
        MethodElement binding = NativeInfo.get(ctxt).getNativeBinding(method);
        return super.interfaceMethodOf(instance, binding != null ? binding : method);
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        FieldElement binding = NativeInfo.get(ctxt).getNativeBinding(field);
        return super.instanceFieldOf(instance, binding != null ? binding : field);
    }

    @Override
    public ValueHandle staticField(FieldElement field) {
        FieldElement binding = NativeInfo.get(ctxt).getNativeBinding(field);
        return super.staticField(binding != null ? binding : field);
    }
}
