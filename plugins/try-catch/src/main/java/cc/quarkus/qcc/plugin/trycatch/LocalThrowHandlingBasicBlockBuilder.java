package cc.quarkus.qcc.plugin.trycatch;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;

/**
 * The basic block builder which handles any "local" {@code throw} to a handler in the same method or function.
 */
public class LocalThrowHandlingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public LocalThrowHandlingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
    }

    public BasicBlock throw_(final Value value) {
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler == null) {
            // propagate to caller
            return super.throw_(value);
        }
        // instead, dispatch to the exception handler
        BasicBlock from = goto_(exceptionHandler.getHandler());
        exceptionHandler.enterHandler(from, value);
        return from;
    }
}
