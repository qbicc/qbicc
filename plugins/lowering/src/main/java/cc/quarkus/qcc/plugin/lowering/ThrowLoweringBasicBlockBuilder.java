package cc.quarkus.qcc.plugin.lowering;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.FunctionType;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        ThrowExceptionHelper teh = ThrowExceptionHelper.get(ctxt);
        Value ptr = addressOf(instanceFieldOf(referenceHandle(currentThread()), teh.getUnwindExceptionField()));
        String functionName = "_Unwind_RaiseException";
        FunctionType functionType = teh.getRaiseExceptionMethod().getType(List.of(/**/));
        ctxt.getImplicitSection(getCurrentElement()).declareFunction(teh.getRaiseExceptionMethod(), functionName, functionType);
        callFunction(ctxt.getLiteralFactory().literalOfSymbol(functionName, functionType), List.of(ptr));
        return unreachable();
    }
}
