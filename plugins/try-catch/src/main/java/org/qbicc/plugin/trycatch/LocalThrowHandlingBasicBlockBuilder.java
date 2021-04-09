package org.qbicc.plugin.trycatch;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;

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
