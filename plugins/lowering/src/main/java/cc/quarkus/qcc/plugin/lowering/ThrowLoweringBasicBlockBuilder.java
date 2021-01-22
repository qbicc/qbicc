package cc.quarkus.qcc.plugin.lowering;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.*;
import cc.quarkus.qcc.object.Function;

import java.util.List;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        Value unwindException = readInstanceField(ctxt.getCurrentThreadValue(), ThrowExceptionHelper.get(ctxt).getUnwindExceptionField(), JavaAccessMode.PLAIN);
        Function function = ctxt.getExactFunction(ThrowExceptionHelper.get(ctxt).getRaiseExceptionMethod());
        super.callFunction(ctxt.getLiteralFactory().literalOfSymbol(function.getName(), function.getType()), List.of(unwindException));
        return unreachable();
    }
}
