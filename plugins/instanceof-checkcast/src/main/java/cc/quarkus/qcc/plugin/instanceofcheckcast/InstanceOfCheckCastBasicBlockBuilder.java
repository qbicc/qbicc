package cc.quarkus.qcc.plugin.instanceofcheckcast;

import java.util.List;
import java.util.ArrayList;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.ValueType;

/**
 * A BasicBlockBuilder which replaces instanceof/checkcast operations with calls to
 * RuntimeHelper APIs.
 */
public class InstanceOfCheckCastBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    static final boolean PLUGIN_DISABLED = true;

    public InstanceOfCheckCastBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value instanceOf(final Value input, final ValueType expectedType) {
        if (PLUGIN_DISABLED) {
            return super.instanceOf(input, expectedType);
        }
        // This code is not yet enabled.  Committing in this state so it's available
        // and so the plugin is included in the list of plugins.


        ctxt.info("Lowering instanceof:" + expectedType.getClass());
        // Value result = super.instanceOf(input, expectedType);
        // convert InstanceOf into a new FunctionCall()
        // RuntimeHelpers.fast_instanceof(CurrentThread, Value, ValueType) {
        //  cheap checks for class depth and then probe supers[]
        //  for array cases, etc, call RuntimeHelpers.slow_instanceOf(CurrentThread, Value, ValueType)
        // and let the optimizer inline the 'fast_instanceof' call and hope the rest is removed
        // mark the slow path as @noinline
        // DelegatingBasicBlockBuilder.getLocation() to get the bci & line
        ClassContext bootstrapCC = ctxt.getBootstrapClassContext();
        DefinedTypeDefinition dtd = bootstrapCC.findDefinedType("cc/quarkus/qcc/runtime/main/VMHelpers");
        if (dtd == null) {
            ctxt.error("Can't find runtime library class: " + "cc/quarkus/qcc/runtime/main/VMHelpers");
        }
        ValidatedTypeDefinition resolved = dtd.validate();

        int idx = resolved.findMethodIndex(e -> "fast_instanceof".equals(e.getName()));
        assert(idx != -1);
        MethodElement methodElement = resolved.getMethod(idx);
        ctxt.registerEntryPoint(methodElement);
        LiteralFactory lf = ctxt.getLiteralFactory();
        Function function = ctxt.getExactFunction(methodElement);
        List<Value> args = List.of(input, lf.literalOfType(expectedType));
        return super.callFunction(lf.literalOfSymbol(function.getName(), function.getType()), args);
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        return super.instanceOf(input, desc);
    }

    // TODO: Find equivalent checkcast methods to implement here as well
}
