package cc.quarkus.qcc.plugin.lowering;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        ThrowExceptionHelper teh = ThrowExceptionHelper.get(ctxt);
        Value ptr = addressOf(instanceFieldOf(referenceHandle(currentThread()), teh.getUnwindExceptionField()));
        invokeStatic(teh.getRaiseExceptionMethod(), List.of(ptr));
        return unreachable();
    }
}
