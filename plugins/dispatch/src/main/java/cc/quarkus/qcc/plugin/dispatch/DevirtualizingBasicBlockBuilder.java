package cc.quarkus.qcc.plugin.dispatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.*;
import cc.quarkus.qcc.type.definition.element.MethodElement;

import java.util.List;

public class DevirtualizingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public DevirtualizingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        if (kind == DispatchInvocation.Kind.VIRTUAL && target.isFinal()) {
            // Handle a very trivial case as a proof of concept that the phase actually does something..
            ctxt.info("I devirtualized a call to " + target);
            return super.invokeInstance(DispatchInvocation.Kind.EXACT, instance, target, arguments);
        }
        return super.invokeInstance(kind, instance, target, arguments);
    }

    @Override
    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        if (kind == DispatchInvocation.Kind.VIRTUAL && target.isFinal()) {
            // Handle a very trivial case as a proof of concept that the phase actually does something..
            ctxt.info("I devirtualized a call to " + target);
            return super.invokeValueInstance(DispatchInvocation.Kind.EXACT, instance, target, arguments);
        }
        return super.invokeValueInstance(kind, instance, target, arguments);
    }
}
