package cc.quarkus.qcc.plugin.native_;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public class NativeBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NativeBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.nativeFunctions.get(target);
        if (functionInfo != null) {
            // todo: store current thread into TLS for recursive Java call-in
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        // no special behaviors
        return super.invokeValueStatic(target, arguments);
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        NativeFunctionInfo functionInfo = nativeInfo.nativeFunctions.get(target);
        if (functionInfo != null) {
            // todo: store current thread into TLS for recursive Java call-in
            return callFunction(functionInfo.symbolLiteral, arguments);
        }
        // no special behaviors
        return super.invokeStatic(target, arguments);
    }
}
