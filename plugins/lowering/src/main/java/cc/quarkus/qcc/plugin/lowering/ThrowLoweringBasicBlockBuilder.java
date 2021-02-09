package cc.quarkus.qcc.plugin.lowering;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.*;
import cc.quarkus.qcc.type.definition.element.MethodElement;

import java.util.List;

public class ThrowLoweringBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThrowLoweringBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        Value unwindException = readInstanceField(currentThread(), ThrowExceptionHelper.get(ctxt).getUnwindExceptionField(), JavaAccessMode.PLAIN);
        MethodElement raiseException = ThrowExceptionHelper.get(ctxt).getRaiseExceptionMethod();
        ctxt.getImplicitSection(getCurrentElement())
            .declareFunction(raiseException, raiseException.getName(), raiseException.getType(List.of()));
        super.callFunction(ctxt.getLiteralFactory().literalOfSymbol(raiseException.getName(), raiseException.getType(List.of())), List.of(unwindException));
        return unreachable();
    }
}
